package com.sunilson.firenote.presentation.homepage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sunilson.firenote.Interfaces.ConfirmDialogResult;
import com.sunilson.firenote.Interfaces.MainActivityInterface;
import com.sunilson.firenote.R;
import com.sunilson.firenote.data.models.Element;
import com.sunilson.firenote.presentation.application.BaseApplication;
import com.sunilson.firenote.presentation.shared.activities.BaseActivity;
import com.sunilson.firenote.presentation.shared.presenters.BaseContract;
import com.sunilson.firenote.presentation.shared.activities.BaseElementActivity;
import com.sunilson.firenote.presentation.bin.BinActivity;
import com.sunilson.firenote.presentation.bundle.BundleActivity;
import com.sunilson.firenote.presentation.checklist.ChecklistActivity;
import com.sunilson.firenote.presentation.note.NoteActivity;
import com.sunilson.firenote.presentation.settings.SettingsActivity;
import com.sunilson.firenote.adapters.CategoryVisibilityAdapter;
import com.sunilson.firenote.adapters.ElementRecyclerAdapter;
import com.sunilson.firenote.presentation.dialogs.AddElementDialog;
import com.sunilson.firenote.presentation.dialogs.EditElementDialog;
import com.sunilson.firenote.presentation.dialogs.ListAlertDialog;
import com.sunilson.firenote.presentation.dialogs.PasswordDialog;
import com.sunilson.firenote.presentation.dialogs.VisibilityDialog;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dagger.android.AndroidInjection;
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter;
import jp.wasabeef.recyclerview.animators.ScaleInAnimator;

import static com.sunilson.firenote.R.id.swipeContainer;

public class MainActivity extends BaseActivity implements MainActivityInterface, ConfirmDialogResult, HomepagePresenterContract.HomepageView {

    private DatabaseReference mReference, mElementsReference, mBinReference;
    private ChildEventListener mElementsListener;
    private ElementRecyclerAdapter elementRecyclerAdapter;
    private CategorySpinnerAdapter spinnerCategoryAdapter;
    private CategoryVisibilityAdapter listCategoryVisibilityAdapter;
    private RecyclerView recyclerView;
    private View.OnClickListener recycleOnClickListener;
    private View.OnLongClickListener recycleOnLongClickListener;
    private CoordinatorLayout coordinatorLayout;
    private FirebaseUser user;
    private TextView currentSortingMethod;
    private List<Category> categories;
    private boolean started;
    private LinearLayoutManager linearLayoutManager;
    private String restoredElement = "";
    private DialogFragment addEditDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Boolean deletedElement = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Set Main Context of BaseApplication, so we can use it in the other Activities
        ((BaseApplication) getApplicationContext()).mainContext = this;

        currentSortingMethod = findViewById(R.id.current_sorting_method);
        coordinatorLayout = findViewById(R.id.activity_main);
        swipeRefreshLayout = findViewById(swipeContainer);


        //Initialize the Firebase Auth System and the User
        user = mAuth.getCurrentUser();

        //Get the users Database Reference, if user exists
        if (user != null) {
            mReference = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());

            if (mReference != null) {

                //Initialize the Listener which detects changes in the note recyclerData
                initializeElementsListener();

                //Register ChildEventListener here so it's not added every time we switch Activity
                mElementsReference = mReference.child("elements").child("main");

                //Category Reference
                initializeCategories();

                //RecyclerView Initialization
                recyclerView = findViewById(R.id.elementList);
                recyclerView.setHasFixedSize(true);
                linearLayoutManager = new LinearLayoutManager(this);
                initializeRecyclerOnClickListener();
                initializeRecyclerOnLongClickListener();

                elementRecyclerAdapter = new ElementRecyclerAdapter(this, recycleOnClickListener, recycleOnLongClickListener, recyclerView);

                AlphaInAnimationAdapter alphaInAnimationAdapter = new AlphaInAnimationAdapter(elementRecyclerAdapter);
                alphaInAnimationAdapter.setFirstOnly(false);
                alphaInAnimationAdapter.setDuration(200);
                recyclerView.setAdapter(alphaInAnimationAdapter);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setItemAnimator(new ScaleInAnimator(new OvershootInterpolator(1f)));
                recyclerView.getItemAnimator().setAddDuration(400);
                ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallbackMain(elementRecyclerAdapter);
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
                itemTouchHelper.attachToRecyclerView(recyclerView);

                //Papierkorb Reference
                mBinReference = mReference.child("bin").child("main");

                if (LocalSettingsManager.getInstance().getSortingMethod() != null) {
                    currentSortingMethod.setText(getString(R.string.current_sorthing_method) + " " + LocalSettingsManager.getInstance().getSortingMethod());
                } else {
                    currentSortingMethod.setText(getString(R.string.current_sorthing_method) + " " + getString(R.string.sort_ascending_name));
                }
                currentSortingMethod.setOnClickListener(view -> {
                    DialogFragment dialog = ListAlertDialog.newInstance(getResources().getString(R.string.menu_sort), "sort", null, null);
                    dialog.show(getSupportFragmentManager(), "dialog");
                });

                swipeRefreshLayout.setOnRefreshListener(() -> refreshListeners());

            }
        }

        //The Button used to add a new Element
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            DialogFragment dialog = ListAlertDialog.newInstance(getResources().getString(R.string.add_Element_Title), "addElement", null, null);
            dialog.show(getSupportFragmentManager(), "dialog");
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!started) {
            if (mElementsReference != null) {
                mElementsReference.addChildEventListener(mElementsListener);
                started = true;
                Handler myHandler = new Handler();
                myHandler.postDelayed(this::showTutorial, 500);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Clear list
        elementRecyclerAdapter.clear();

        //Remove listener
        if (mElementsReference != null) {
            mElementsReference.removeEventListener(mElementsListener);
        }
    }

    /**
     * Go to home screen on back press
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logOut) {
            mAuth.signOut();
            return true;
        } else if (id == R.id.main_element_sort) {
            DialogFragment dialog = ListAlertDialog.newInstance(getResources().getString(R.string.menu_sort), "sort", null, null);
            dialog.show(getSupportFragmentManager(), "dialog");
        } else if (id == R.id.main_element_visibility) {
            VisibilityDialog visibilityDialog = new VisibilityDialog();
            visibilityDialog.show(getSupportFragmentManager(), "dialog");
        } else if (id == R.id.action_bin) {
            Intent i = new Intent(MainActivity.this, BinActivity.class);
            startActivity(i);
        } else if (id == R.id.action_settings) {
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Click listener for the element list
     */
    private void initializeRecyclerOnClickListener() {
        recycleOnClickListener = v -> {
            //Get element at click position from list adapter
            int itemPosition = recyclerView.getChildLayoutPosition(v);
            Element element = elementRecyclerAdapter.getItem(itemPosition);

            //Check if element is locked
            if (element.getLocked()) {
                DialogFragment dialogFragment = PasswordDialog.newInstance("passwordOpenElement", null, element.getElementID(), null, 0);
                dialogFragment.show(getSupportFragmentManager(), "dialog");
            } else {
                //Start correct activity
                Intent i = null;
                switch (element.getNoteType()) {
                    case "checklist":
                        i = new Intent(MainActivity.this, ChecklistActivity.class);
                        break;
                    case "note":
                        i = new Intent(MainActivity.this, NoteActivity.class);
                        break;
                    case "bundle":
                        i = new Intent(MainActivity.this, BundleActivity.class);
                        break;
                }

                //Put element recyclerData into the intent and start it
                if (i != null) {
                    i.putExtra("elementID", element.getElementID());
                    i.putExtra("elementTitle", element.getTitle());
                    i.putExtra("elementColor", element.getColor());
                    i.putExtra("elementType", element.getNoteType());
                    i.putExtra("categoryID", element.getCategory().getCategoryID());
                    startActivity(i);
                }
            }
        };
    }

    /**
     * Show edit dialog on long click
     */
    private void initializeRecyclerOnLongClickListener() {
        recycleOnLongClickListener = v -> {
            int itemPosition = recyclerView.getChildLayoutPosition(v);
            Element element = elementRecyclerAdapter.getItem(itemPosition);

            if (element.getLocked()) {
                DialogFragment dialogFragment = PasswordDialog.newInstance("passwordEditElement", element.getNoteType(), element.getElementID(), element.getTitle(), 0);
                dialogFragment.show(getSupportFragmentManager(), "dialog");
            } else {
                DialogFragment dialog = ListAlertDialog.newInstance(getResources().getString(R.string.edit_element_title), "editElement", element.getElementID(), element.getNoteType());
                dialog.show(getSupportFragmentManager(), "dialog");
            }
            return true;
        };
    }

    /**
     * Listener for the main element list
     */
    private void initializeElementsListener() {
        mElementsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Element element = dataSnapshot.getValue(Element.class);
                if (element != null) {
                    element.setElementID(dataSnapshot.getKey());
                    //Check if element is valid, else remove it
                        int position = elementRecyclerAdapter.add(element);
                        //If element was restored, scroll to position of element
                        if (element.getElementID().equals(restoredElement)) {
                            recyclerView.smoothScrollToPosition(position);
                            restoredElement = "";
                        }

                        //mElementsReference.child(dataSnapshot.getKey()).removeValue()
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Element element = dataSnapshot.getValue(Element.class);
                if (element != null) {
                    element.setElementID(dataSnapshot.getKey());
                    //Check if element is valid, else remove it
                    if (element.getNoteType() != null) {
                        elementRecyclerAdapter.update(element, element.getElementID());
                    } else {
                        mElementsReference.child(dataSnapshot.getKey()).removeValue();
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                final Element element = dataSnapshot.getValue(Element.class);
                if (element != null) {
                    element.setElementID(dataSnapshot.getKey());
                    if (element.getNoteType() != null) {
                        //Check if open Activity is child of Bundle and removed element is a bundle
                        //If yes, start Main Activity to finish all other activities
                        if (element.getNoteType().equals("bundle")) {
                            Activity currentActivity = ((BaseApplication) getApplicationContext()).getCurrentActivity();
                            if (currentActivity instanceof NoteActivity || currentActivity instanceof ChecklistActivity) {
                                BaseElementActivity baseElementActivity = (BaseElementActivity) currentActivity;
                                if (baseElementActivity.parentID != null) {
                                    Intent i = new Intent(baseElementActivity, MainActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(i);
                                }
                            }
                        }

                        //Remove element from list
                        elementRecyclerAdapter.remove(element.getElementID());
                        //Add element to bin
                        mBinReference.child(element.getElementID()).setValue(element);
                        //If element was deleted by us, show restore snackbar dialog
                        if (deletedElement) {
                            deletedElement = false;
                            Snackbar snackbar = Snackbar
                                    .make(coordinatorLayout, R.string.moved_to_bin, 4000)
                                    .setAction(R.string.undo, view -> {
                                        //Undo deletion. Remove from bin and add to "elements"
                                        Snackbar snackbar1 = Snackbar.make(coordinatorLayout, R.string.element_restored, Snackbar.LENGTH_SHORT);
                                        snackbar1.show();
                                        mBinReference.child(element.getElementID()).removeValue();
                                        mElementsReference.child(element.getElementID()).setValue(element);
                                        restoredElement = element.getElementID();
                                    });
                            snackbar.show();
                        }
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
    }

    /**
     * Set up the category list
     */
    private void initializeCategories() {
        categories = new ArrayList<>();

        categories.add(new Category(getString(R.string.category_business), "business"));
        categories.add(new Category(getString(R.string.category_events), "events"));
        categories.add(new Category(getString(R.string.category_finances), "finances"));
        categories.add(new Category(getString(R.string.category_general), "general"));
        categories.add(new Category(getString(R.string.category_hobbies), "hobbies"));
        categories.add(new Category(getString(R.string.category_holidays), "holidays"));
        categories.add(new Category(getString(R.string.category_project), "project"));
        categories.add(new Category(getString(R.string.category_school), "school"));
        categories.add(new Category(getString(R.string.category_shopping), "shopping"));
        categories.add(new Category(getString(R.string.category_sport), "sport"));

        //Sort the categories by name (needed because of translations)
        Comparator<Category> comparator = (category, t1) -> {
            if (category.getCategoryName().compareToIgnoreCase(t1.getCategoryName()) < 0) {
                return -1;
            } else if (category.getCategoryName().compareToIgnoreCase(t1.getCategoryName()) > 0) {
                return 1;
            }
            return 0;
        };
        Collections.sort(categories, comparator);

        spinnerCategoryAdapter = new CategorySpinnerAdapter(this, R.layout.spinner_item, categories);
        spinnerCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listCategoryVisibilityAdapter = new CategoryVisibilityAdapter(this, R.layout.category_list_layout, categories);
    }

    @Override
    public ElementRecyclerAdapter getElementAdapter() {
        return elementRecyclerAdapter;
    }

    @Override
    public TextView getSortTextView() {
        return currentSortingMethod;
    }

    @Override
    public DatabaseReference getElementsReference() {
        return mElementsReference;
    }

    @Override
    public DatabaseReference getReference() {
        return mReference;
    }

    /**
     * Clears element list and re-attaches the listener so new recyclerData will be loaded
     */
    @Override
    public void refreshListeners() {
        if (mElementsListener != null) {
            if (!((BaseApplication) getApplicationContext()).getInternetConnected()) {
                Toast.makeText(this, R.string.listener_no_connection, Toast.LENGTH_LONG).show();
            }
            mElementsReference.removeEventListener(mElementsListener);
            elementRecyclerAdapter.clear();
            mElementsReference.addChildEventListener(mElementsListener);
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void removeListeners() {
        if (mElementsListener != null) {
            mElementsReference.removeEventListener(mElementsListener);
        }
    }

    @Override
    public CategorySpinnerAdapter getSpinnerCategoryAdapter() {
        return spinnerCategoryAdapter;
    }

    public CategoryVisibilityAdapter getListCategoryVisibilityAdapter() {
        return listCategoryVisibilityAdapter;
    }

    @Override
    public DatabaseReference getBinReference() {
        return mBinReference;
    }

    /**
     * Result from confirm dialog
     *
     * @param bool
     * @param type
     * @param args
     */
    @Override
    public void confirmDialogResult(boolean bool, String type, Bundle args) {
        switch (type) {
            //Open element after password was entered correctly
            case "passwordOpenElement":
                if (bool) {
                    Intent i = null;
                    Element element = elementRecyclerAdapter.getElement(args.getString("elementID"));
                    switch (element.getNoteType()) {
                        case "checklist":
                            i = new Intent(MainActivity.this, ChecklistActivity.class);
                            break;
                        case "note":
                            i = new Intent(MainActivity.this, NoteActivity.class);
                            break;
                        case "bundle":
                            i = new Intent(MainActivity.this, BundleActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            break;
                    }

                    if (i != null) {
                        i.putExtra("elementID", element.getElementID());
                        i.putExtra("elementTitle", element.getTitle());
                        i.putExtra("elementColor", element.getColor());
                        i.putExtra("elementType", element.getNoteType());
                        i.putExtra("categoryID", element.getCategory().getCategoryID());
                        startActivity(i);
                    }
                } else {
                    Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show();
                }
                break;
            //Edit element after password was entered correctly
            case "passwordEditElement":
                if (bool) {
                    DialogFragment dialog = ListAlertDialog.newInstance(getResources().getString(R.string.edit_element_title), "editElement", args.getString("elementID"), args.getString("elementType"));
                    dialog.show(getSupportFragmentManager(), "dialog");
                } else {
                    Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show();
                }
                break;
            case "addElement":
                if (bool) {
                    addEditDialog = AddElementDialog.newInstance(getString(R.string.add_Element_Title), args.getString("elementType"));
                    addEditDialog.show(getSupportFragmentManager(), "dialog");
                }
                break;
            case "editElement":
                if (bool) {
                    addEditDialog = EditElementDialog.newInstance(getString(R.string.edit_element_title), args.getString("elementType"), args.getString("elementID"));
                    addEditDialog.show(getSupportFragmentManager(), "dialog");
                }
                break;
        }
    }

    /**
     * Starting tutorial at first app start
     */
    private void showTutorial() {
        ShowcaseView.Builder showCaseView = new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setTarget(new ViewTarget(R.id.fab, this))
                .setContentTitle(getString(R.string.welcome_to_firenote))
                .setContentText(getString(R.string.tutorial_add_elements))
                .hideOnTouchOutside()
                .setStyle(R.style.ShowCaseViewStyle)
                .singleShot(1)
                .setShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {
                        findViewById(R.id.fab).setEnabled(true);
                        findViewById(R.id.main_element_sort).setEnabled(false);
                        new ShowcaseView.Builder(MainActivity.this)
                                .withMaterialShowcase()
                                .setTarget(new ViewTarget(R.id.main_element_sort, MainActivity.this))
                                .setContentTitle(getString(R.string.tutorial_title_sort))
                                .setContentText(getString(R.string.tutorial_sort))
                                .hideOnTouchOutside()
                                .setStyle(R.style.ShowCaseViewStyle)
                                .setShowcaseEventListener(new OnShowcaseEventListener() {
                                    @Override
                                    public void onShowcaseViewHide(ShowcaseView showcaseView) {
                                        findViewById(R.id.main_element_sort).setEnabled(true);
                                        if (findViewById(R.id.main_element_visibility) != null) {
                                            findViewById(R.id.main_element_visibility).setEnabled(false);
                                            new ShowcaseView.Builder(MainActivity.this)
                                                    .withMaterialShowcase()
                                                    .setTarget(new ViewTarget(R.id.main_element_visibility, MainActivity.this))
                                                    .setContentTitle(getString(R.string.tutorial_title_hide))
                                                    .setContentText(getString(R.string.tutorial_hide))
                                                    .hideOnTouchOutside()
                                                    .setStyle(R.style.ShowCaseViewStyle)
                                                    .setShowcaseEventListener(new OnShowcaseEventListener() {
                                                        @Override
                                                        public void onShowcaseViewHide(ShowcaseView showcaseView) {
                                                            findViewById(R.id.main_element_visibility).setEnabled(true);
                                                        }

                                                        @Override
                                                        public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                                                        }

                                                        @Override
                                                        public void onShowcaseViewShow(ShowcaseView showcaseView) {

                                                        }

                                                        @Override
                                                        public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                                                        }
                                                    })
                                                    .build();
                                        }
                                    }

                                    @Override
                                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                                    }

                                    @Override
                                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                                    }

                                    @Override
                                    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                                    }
                                })
                                .build();
                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {
                        findViewById(R.id.fab).setEnabled(false);
                    }

                    @Override
                    public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {

                    }
                });

        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        int margin = ((Number) (getResources().getDisplayMetrics().density * 16)).intValue();
        lps.setMargins(margin, margin, margin, margin);
        showCaseView.build().setButtonPosition(lps);
    }

    @Override
    public void setDeletedElement(boolean value) {
        deletedElement = value;
    }


    @Override
    public void addObserver(@NotNull BaseContract.IBasePresenter presenter) {

    }
}

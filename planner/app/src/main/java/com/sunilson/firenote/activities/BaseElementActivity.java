package com.sunilson.firenote.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sunilson.firenote.BaseApplication;
import com.sunilson.firenote.Interfaces.ConfirmDialogResult;
import com.sunilson.firenote.Interfaces.ElementInterface;
import com.sunilson.firenote.Interfaces.MainActivityInterface;
import com.sunilson.firenote.LocalSettingsManager;
import com.sunilson.firenote.R;
import com.sunilson.firenote.baseClasses.Element;
import com.sunilson.firenote.dialogs.EditElementDialog;
import com.sunilson.firenote.dialogs.ListAlertDialog;

/**
 * @author Linus Weiss
 */

public abstract class BaseElementActivity extends BaseActivity implements ElementInterface, ConfirmDialogResult {

    private MenuItem lockButton;
    protected boolean locked, started;
    protected int elementColor;
    protected EditText titleEditText;
    protected ImageView titleDoneButton;
    protected String elementID, elementType, elementTitle, parentID, categoryID;
    protected ValueEventListener mElementListener, mFinishedListener;
    protected FirebaseUser user;
    protected DatabaseReference mElementReference, mContentReference;
    protected InputMethodManager imm;
    protected boolean titleEdit = false;
    protected DialogFragment addEditDialog;

    /*
    ------------------------
    ---- Android Events ----
    ------------------------
     */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        elementType = i.getStringExtra("elementType");

        switch (elementType) {
            case "checklist":
                setContentView(R.layout.activity_checklist);
                break;
            case "note":
                setContentView(R.layout.activity_note);
                break;
            case "bundle":
                setContentView(R.layout.activity_bundle);
                break;
        }

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        titleDoneButton = (ImageView) toolbar.findViewById(R.id.title_done_button);
        titleEditText = (EditText) toolbar.findViewById(R.id.title_edittext);
        titleEditText.setFocusable(false);
        titleEditText.setFocusableInTouchMode(false);

        //Get Element ID from clicked element and set Title
        elementID = i.getStringExtra("elementID");
        parentID = i.getStringExtra("parentID");
        elementColor = i.getIntExtra("elementColor", 1);
        categoryID = i.getStringExtra("categoryID");
        titleEditText.setText(i.getStringExtra("elementTitle"));

        //Firebase Authentication
        user = mAuth.getCurrentUser();

        //Firebase Reference to the Checklist element we are currently in and the contents of that element
        if (user != null) {
            if (parentID == null) {
                mElementReference = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("elements").child("main").child(elementID);
            } else {
                mElementReference = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("elements").child("bundles").child(parentID).child(elementID);
            }
            mContentReference = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("contents").child(elementID);
        }

        if (mElementReference != null) {
            initializeElementListener();
            initializeFinishedListener();
            setColors();

            titleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (i == EditorInfo.IME_ACTION_DONE)) {
                        titleDoneButton.performClick();
                    }
                    return false;
                }
            });

            titleDoneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopTitleEdit();
                }
            });

            titleEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!titleEdit) {
                        titleEditStarted();
                        startTitleEdit();
                    }
                }
            });
        }
    }



    protected void startTitleEdit() {
        titleEdit = true;
        titleEditText.setEllipsize(null);
        titleEditText.setFocusableInTouchMode(true);
        titleEditText.setFocusable(true);
        titleEditText.requestFocus();
        titleEditText.setSelection(titleEditText.getText().length());
        titleEditText.setBackgroundColor(ContextCompat.getColor(BaseElementActivity.this, R.color.tint_white));
        titleEditText.setTextColor(ContextCompat.getColor(BaseElementActivity.this, R.color.title_text_color));
        titleDoneButton.setVisibility(View.VISIBLE);
        imm.showSoftInput(titleEditText, InputMethodManager.SHOW_FORCED);
    }

    protected void stopTitleEdit() {
        titleEdit = false;
        titleEditText.setEllipsize(TextUtils.TruncateAt.END);
        titleEditText.setSelection(0);
        titleEditText.setFocusableInTouchMode(false);
        titleEditText.setFocusable(false);
        titleEditText.setBackgroundColor(ContextCompat.getColor(BaseElementActivity.this, android.R.color.transparent));
        titleEditText.setTextColor(ContextCompat.getColor(BaseElementActivity.this, R.color.tint_white));
        titleDoneButton.setVisibility(View.GONE);
        imm.hideSoftInputFromWindow(titleEditText.getWindowToken(), 0);
        mElementReference.child("title").setValue(titleEditText.getText().toString());
        titleEditStopped();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    protected void titleEditStarted() {

    }

    protected void titleEditStopped() {

    }

    @Override
    protected void onStart() {
        super.onStart();

        if(!started) {
            started = true;
            Handler myHandler = new Handler();
            myHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showTutorial();
                    }
                }, 500);
            //Check if parent has been deleted
            if (parentID != null) {
                DatabaseReference dRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).child("elements").child("main").child(parentID);
                dRef.addListenerForSingleValueEvent(mFinishedListener);
            }

            if (locked) {
                if (lockButton != null) {
                    lockButton.setIcon(ContextCompat.getDrawable(this, R.drawable.element_lock_icon));
                }
            } else {
                if (lockButton != null) {
                    lockButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_lock_open_white_24dp));
                }
            }

            mElementReference.addValueEventListener(mElementListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mElementReference.child("title").setValue(titleEditText.getText().toString());
        if (mElementListener != null) {
            mElementReference.removeEventListener(mElementListener);
        }
    }

    /*
    public void removeListeners() {
        if (mElementListener != null) {
            mElementReference.removeEventListener(mElementListener);
        }
    }
    */

    protected void setColors() {
        ColorDrawable colorDrawable = new ColorDrawable();
        colorDrawable.setColor(elementColor);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(colorDrawable);
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(elementColor));
        }

        //Darken notification bar color and set it to status bar. Only works in Lollipop and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            float[] hsv = new float[3];
            Color.colorToHSV(elementColor, hsv);
            hsv[2] *= 0.6f;
            int darkenedColor = Color.HSVToColor(hsv);

            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(darkenedColor);
        }
    }

     /*
    -----------------------------
    --- Listener Initializing ---
    -----------------------------
     */

    protected void initializeElementListener() {
        mElementListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Element element = dataSnapshot.getValue(Element.class);
                if (element != null) {
                    if (element.getNoteType() != null) {
                        updateElement(element);
                    } else {
                        mElementReference.removeValue();
                        ((MainActivityInterface) ((BaseApplication) getApplicationContext()).mainContext).refreshListeners();
                        finish();
                        Log.i("Linus", "finish");
                    }
                } else {
                    ((MainActivityInterface) ((BaseApplication) getApplicationContext()).mainContext).refreshListeners();
                    finish();
                    Log.i("Linus", "finish");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void updateElement(Element element) {
        //Set Title
        setTitle(element.getTitle());
        elementTitle = element.getTitle();
        titleEditText.setText(element.getTitle());
        categoryID = element.getCategoryID();

        //Check Locked
        locked = element.getLocked();

        if (locked) {
            if (lockButton != null) {
                lockButton.setIcon(ContextCompat.getDrawable(this, R.drawable.element_lock_icon));
            }
        } else {
            if (lockButton != null) {
                lockButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_lock_open_white_24dp));
            }
        }

        //Set colors
        elementColor = element.getColor();
        setColors();
    }

    private void initializeFinishedListener() {
        mFinishedListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Element element = dataSnapshot.getValue(Element.class);
                if (element == null) {
                    finish();
                } else {
                    if (element.getNoteType() == null) {
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (titleEdit) {
                stopTitleEdit();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_lock) {
            if (!LocalSettingsManager.getInstance().getMasterPassword().equals("")) {
                mElementReference.child("locked").setValue(!locked);
            } else {
                Toast.makeText(this, R.string.master_password_not_set, Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.menu_settings) {
            DialogFragment dialog = ListAlertDialog.newInstance(getResources().getString(R.string.edit_element_title), "editElement", elementID, elementType);
            dialog.show(getSupportFragmentManager(), "dialog");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        lockButton = menu.findItem(R.id.menu_lock);

        if (locked) {
            if (lockButton != null) {
                lockButton.setIcon(ContextCompat.getDrawable(this, R.drawable.element_lock_icon));
            }
        } else {
            if (lockButton != null) {
                lockButton.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_lock_open_white_24dp));
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public int getElementColor() {
        return elementColor;
    }

    @Override
    public DatabaseReference getElementReference() {
        return mElementReference;
    }

    @Override
    public DatabaseReference getContentsReference() {
        return mContentReference;
    }

    @Override
    public String getElementTitle() {
        return elementTitle;
    }

    @Override
    public String getElementCategoryID() {
        return categoryID;
    }

    @Override
    public void stopListeners() {
        if (mElementListener != null) {
            mElementReference.removeEventListener(mElementListener);
        }
    }

    @Override
    public void confirmDialogResult(boolean bool, String type, Bundle args) {
        if (type.equals("editElement")) {
            if (bool) {
                addEditDialog = EditElementDialog.newInstance(getString(R.string.edit_element_title), args.getString("elementType"), args.getString("elementID"));
                addEditDialog.show(getSupportFragmentManager(), "dialog");
            }
        }
    }

    //Placeholder to be overriden
    protected void showTutorial() {

    }
}
package com.commandcenter.devchat.Controller;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.commandcenter.devchat.Model.User;
import com.commandcenter.devchat.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ActivityRegister extends AppCompatActivity {

    EditText et_email, et_password, et_username;
    Button btn_register, btn_cancel;
    EditText et_gender;

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;

    String[] values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        values = new String[3];
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference("users");

        et_email     = findViewById(R.id.register_et_email);
        et_password  = findViewById(R.id.register_et_password);
        et_username  = findViewById(R.id.register_et_username);
        et_gender = findViewById(R.id.register_et_gender);
        btn_cancel   = findViewById(R.id.register_btnCancel);
        btn_register = findViewById(R.id.register_btnRegister);

        Intent intent = getIntent();
        Bundle details = intent.getExtras();

        if (details != null) {
            values = intent.getStringArrayExtra("details");
            et_email.setText(values[0]);
            et_password.setText(values[1]);

        }else {
            values[0] = et_email.getText().toString();
            values[1] = et_password.getText().toString();
            values[2] = et_username.getText().toString();

        }

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignUp(et_email.getText().toString(), et_password.getText().toString());
            }
        });
    }

    private void SignUp(String email, String password) {
        //create a new user account
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ) {

        }else {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(ActivityRegister.this, "User Account Created!", Toast.LENGTH_SHORT).show();
                                createUser(et_username.getText().toString());
                                Intent signInIntent = new Intent(ActivityRegister.this, MainActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putStringArray("values", values);
                                signInIntent.putExtras(bundle);
                                startActivity(signInIntent);
                            } else {
                                Toast.makeText(ActivityRegister.this, "Error Creating New User Account!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void createUser(String username) {
        //save the username to use in the chatbox activity
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Username Field Required!", Toast.LENGTH_SHORT).show();
            return;
        }else {
            FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
            String userID = current_user.getUid();
            User user = new User(et_username.getText().toString(), et_gender.getText().toString(), "Newbie", "Online", "Active", userID);
            mRef.child(mAuth.getCurrentUser().getUid()).setValue(user);
        }
    }
}

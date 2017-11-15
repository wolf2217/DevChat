package com.commandcenter.devchat.Controller;


import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.commandcenter.devchat.Adapter.FirebaseMessageAdapter;
import com.commandcenter.devchat.Model.ChatboxMessage;
import com.commandcenter.devchat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

public class Chatbox_Activity extends AppCompatActivity {

    private EditText et_message;
    private Button btnSend;
    private TextView incoming_msg;

    //recyclerview
    private RecyclerView messageRecView;

    //list of messages
    private List<ChatboxMessage> messageList;
    private List<String> userList;
    private FirebaseMessageAdapter messageAdapter;
    //Firebase
    private FirebaseDatabase mDatabase;
   // private DatabaseReference mDataRef;
    private DatabaseReference mNewMessageRef;
    private DatabaseReference mUsers;
    private DatabaseReference mAllUsers;
    private DatabaseReference mIncomingMsg;
    private FirebaseAuth mAuth;

    public String user;
    private String userID;
    private String rank;
    private String status;
    private String chatStatus;
    private String isTyping = "";

    private String curDate;
    private String time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbox);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            Init();
            //Load messages from current date

            mNewMessageRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    messageList.clear();
                    Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                    for(DataSnapshot child : children) {
                        ChatboxMessage message = child.getValue(ChatboxMessage.class);
                        getStatus(message.getUser());
                        if (!messageList.contains(message)) {
                            messageList.add(message);
                        }
                    }
                    messageAdapter = new FirebaseMessageAdapter(getApplicationContext(), messageList);
                    RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(Chatbox_Activity.this);
                    messageRecView.setLayoutManager(mLayoutManager);
                    messageRecView.setItemAnimator(new DefaultItemAnimator());
                    messageRecView.setAdapter(messageAdapter);
                    mLayoutManager.scrollToPosition(messageList.size() - 1);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


            mUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    user = dataSnapshot.child("username").getValue().toString();
                    rank = dataSnapshot.child("rank").getValue().toString();
                    status = dataSnapshot.child("status").getValue().toString();
                    chatStatus = dataSnapshot.child("chatStatus").getValue().toString();
                    welcomeUser(user);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }else {
            Intent intent = new Intent(Chatbox_Activity.this, MainActivity.class);
            startActivity(intent);
        }
        //Button click event
        btnSend =  findViewById(R.id.chatbox_btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(et_message.getText().toString())) {
                    Toast.makeText(Chatbox_Activity.this, "Please enter a message to send!", Toast.LENGTH_SHORT).show();
                }else {
                    SimpleDateFormat dFormat = new SimpleDateFormat("hh:mm:ss a");
                    dFormat.setTimeZone(TimeZone.getDefault());
                    time = dFormat.format(new Date()).toString();

                        ChatboxMessage message = new ChatboxMessage(user, et_message.getText().toString(), rank,  curDate, time);
                        getStatus(user);
                        processMessage(message, chatStatus);

                }
            }
        });
        //this removes the specific node and all the child nodes from firebase
        // mNewMessageRef.removeValue();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (mAuth.getCurrentUser() != null) {

            switch (item.getItemId()) {
                case R.id.logout:
                    Toast.makeText(this, "User signed out!", Toast.LENGTH_SHORT).show();
                    signOut();
                    return true;
                case R.id.navFriends:

                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        mUsers.child("status").setValue("Offline");
        mAuth.signOut();
        Intent intent = new Intent(Chatbox_Activity.this, MainActivity.class);
        startActivity(intent);

    }

    private void Init() {

        curDate = setDate();
        mDatabase = FirebaseDatabase.getInstance();
        //  mDataRef = mDatabase.getReference("messages");
        mNewMessageRef = mDatabase.getReference("messages").child(curDate);
        mUsers = mDatabase.getReference("users").child(mAuth.getCurrentUser().getUid());
        mAllUsers = mDatabase.getReference("users");

        messageRecView = findViewById(R.id.chatbox_recView);
        et_message = findViewById(R.id.chatbox_et_message);
        incoming_msg = findViewById(R.id.chatbox_incoming);
        messageList = new ArrayList<>();
        userList = new ArrayList<>();

    }
    
    //Create the value listener for incoming messages
    mIncomingMsg.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mIncomingMsg = mDatabase.getReference().child("Typing");

                isTyping = dataSnapshot.getValue().toString();

                if (isTyping.equalsIgnoreCase("True")){
                    incoming_msg.setText("Someone Is Typing...");
                }else{
                    incoming_msg.setText("");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    //End value listener
    
    //Create message textChanged event listener
    et_message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!TextUtils.isEmpty(et_message.getText())){
                    mIncomingMsg.setValue("True");
                    mIncomingMsg.setValue("True");
                    incoming_msg.setText("Someone Is Typing");
                }else{
                    mIncomingMsg.setValue("False");
                    incoming_msg.setText("");
                }
            }
        });
    //End textChange event listener

    //get current user
    private String getUser() {

        mUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String thisUser = dataSnapshot.child("username").getValue().toString();
                user = thisUser;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return user;
    }
    //set time zone for the date
    private String setDate() {

        String thisDate = "";
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Florida"));
        DateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy");
        formatter.setTimeZone(TimeZone.getTimeZone("America/Florida"));
        thisDate = formatter.format(cal.getTime());
        return thisDate;

    }
    // process message before it leaves
    private void processMessage(final ChatboxMessage message, String chatstatus) {

        String[] ban_words = new String[] {"shit", "fuck", "dick", "pussy", "asshole", "ass"};
        String curMessage = message.getChatMessage();//gets message
        String curUser = message.getUser();//gets user
        String curRank = message.getRank();//gets rank

        switch(chatstatus) {
            case "Active":// user has full access to chat
                switch (curRank) {
                       case "Admin"://user is Admin and has more privilages
                       case "Owner":
                       case "Moderator":
                            if (curMessage.startsWith("~")) {
                                String[] messageDetails = curMessage.split(" ");//split the current message on the space to get the command(param 0), the username(param 1) i.e ~ban Inked
                                switch (messageDetails[0]) {
                                    //commands
                                    case "~ban":
                                        //set user status
                                        //setStatus(messageDetails[1], "Ban");
                                        SimpleDateFormat dFormat = new SimpleDateFormat("hh:mm:ss a");
                                        dFormat.setTimeZone(TimeZone.getDefault());
                                        String curtime = dFormat.format(new Date()).toString();
                                        ChatboxMessage banMessage = new ChatboxMessage(curUser, messageDetails[1] + " has been Banned by Admin", curRank, curDate, curtime);
                                        mNewMessageRef.push().setValue(banMessage);
                                        break;
                                    case "~kick":
                                        mUsers.child("chatStatus").setValue("Kick");
                                        dFormat = new SimpleDateFormat("hh:mm:ss a");
                                        dFormat.setTimeZone(TimeZone.getDefault());
                                        curtime = dFormat.format(new Date()).toString();
                                        banMessage = new ChatboxMessage(curUser, curUser + " has been Kicked by Admin", curRank, curDate, curtime);
                                        mNewMessageRef.push().setValue(banMessage);
                                        break;
                                    case "~silence":
                                        mUsers.child("chatStatus").setValue("Silence");
                                        dFormat = new SimpleDateFormat("hh:mm:ss a");
                                        dFormat.setTimeZone(TimeZone.getDefault());
                                        curtime = dFormat.format(new Date()).toString();
                                        banMessage = new ChatboxMessage(curUser, curUser + " has been Silenced by Admin", curRank, curDate, curtime);
                                        mNewMessageRef.push().setValue(banMessage);
                                        break;
                                    case "~warn":
                                       // mUsers.child("chatStatus").setValue("Kick");
                                        dFormat = new SimpleDateFormat("hh:mm:ss a");
                                        dFormat.setTimeZone(TimeZone.getDefault());
                                        curtime = dFormat.format(new Date()).toString();
                                        banMessage = new ChatboxMessage(curUser, curUser + " this is a warning, there won't be another!", curRank, curDate, curtime);
                                        mNewMessageRef.push().setValue(banMessage);

                                        break;
                                }
                            } else {
                                mNewMessageRef.push().setValue(message);
                                et_message.setText("");
                            }
                        break;

                        default: //all other user privilages just send the message , when we decide to add more ranks we will put them here to process.
                             //maybe we can add different color text for different ranks.
                        for (String word : curMessage.split(" ")) {
                            for (int i = 0; i < ban_words.length; i++) {
                                if (word.equalsIgnoreCase(ban_words[i])) {
                                    curMessage = curMessage.replace(word, "piggy");
                                }
                            }
                        }
                        ChatboxMessage newMessage = new ChatboxMessage(message.getUser(), curMessage, message.getRank(), message.getDate(), message.getTime());
                        mNewMessageRef.push().setValue(newMessage);
                        et_message.setText("");
                        break;
                }
            break;
            //end Online Status
            case "Ban"://user is banned from chatting, any one of these 3 case and the user gets a toast message.
            case "Silence":
            case "Kick":
                String toastMessage = message.getUser() + ", you do not have chat privilages.\r\nLet DevChat Admin know why you should get chat privilages restored";
               // Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle("Privilages Revoked");

                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                alertBuilder.setView(input);
                alertBuilder.setMessage(toastMessage)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String email = "glarosa001@tampabay.rr.com";
                                String subject = "DevChat Banned Account";
                                String emailMessage = "User [" + message.getUser() + "] would Like Their Account Privilages Restored!\r\nReason : " + input.getText().toString();
                                EmailHelper emailHelper = new EmailHelper(Chatbox_Activity.this, email, subject, emailMessage);
                                emailHelper.sendEmail(email, subject, emailMessage);
                            }
                        });
                AlertDialog alert = alertBuilder.create();
                alert.show();
                et_message.setText("");
                break;

        }
    }

    private void welcomeUser(String username) {

        SimpleDateFormat dFormat = new SimpleDateFormat("hh:mm:ss a");
        String curTime = dFormat.format(new Date()).toString();

        ChatboxMessage message = new ChatboxMessage("DevChat Bot", "Welcome to DevChat : " + user + " is now ONLINE!", "Moderator", curDate, curTime);
        mNewMessageRef.push().setValue(message);
    }

    private int generateID() {

        int id;
        Random rand = new Random();
        int min = 000000;
        int max = 999999;
        id = rand.nextInt((max - min) + 1) + min;

        return id;
    }

    private void setStatus(String user, String status) {

        switch (status) {
            case "Online":
                final String onLineStatus = status;
                chatStatus = status;
                Query onlineQuery = mAllUsers
                        .orderByChild("username")
                        .equalTo(user);
                onlineQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot users : dataSnapshot.getChildren()) {
                            users.getRef().child("status").setValue(onLineStatus);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                break;
            case "Offline":
               final String offLineStatus = status;
                chatStatus = status;
                Query offlineQuery = mAllUsers
                        .orderByChild("username")
                        .equalTo(user);
                offlineQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot users : dataSnapshot.getChildren()) {
                            users.getRef().child("status").setValue(offLineStatus);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                break;
            case "Active":

                break;

            case "Ban":
            case "Silence":
            case "Kick":

                break;
            default:

        }
    }

    private void getStatus(String user) {

        Query query = mAllUsers
                .orderByChild("username")
                .equalTo(user);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot users : dataSnapshot.getChildren()) {
                   chatStatus =  users.child("chatStatus").getValue().toString();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}

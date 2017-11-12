package com.commandcenter.devchat.Controller;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.EditText;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.commandcenter.devchat.Adapter.FirebaseMessageAdapter;
import com.commandcenter.devchat.Model.ChatboxMessage;
import com.commandcenter.devchat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import java.util.TimeZone;

public class Chatbox_Activity extends AppCompatActivity {

    private EditText et_message;
    private Button btnSend;

    //recyclerview
    private RecyclerView messageRecView;

    //list of messages
    private List<ChatboxMessage> messageList;
    private List<String> userList;
    private FirebaseMessageAdapter messageAdapter;
    //Firebase
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDataRef;
    private DatabaseReference mNewMessageRef;
    private DatabaseReference mUsers;
    private FirebaseAuth mAuth;

    private String user;
    private String rank;

    private String curDate;
    private String time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbox);
        //Notification
        notification = new NotificationCompat.Builder(this);
        notification.setAutoCancel(true);

        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mDataRef = mDatabase.getReference("messages");
        mNewMessageRef = mDatabase.getReference("messages");
        mUsers = mDatabase.getReference("users");

        messageRecView = findViewById(R.id.chatbox_recView);
        et_message = findViewById(R.id.chatbox_et_message);
        messageList = new ArrayList<>();
        userList = new ArrayList<>();

        curDate = setDate();
        //Button click event
        btnSend =  findViewById(R.id.chatbox_btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               curDate = setDate();
                if (TextUtils.isEmpty(et_message.getText().toString())) {

                }else {
                    SimpleDateFormat dFormat = new SimpleDateFormat("hh/mm/ss a");
                    dFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    time = dFormat.format(new Date()).toString();
                    ChatboxMessage message = new ChatboxMessage(user, et_message.getText().toString(), rank,  curDate, time);
                    processMessage(user, message);
                    et_message.setText("");
                }
            }
        });
        //this removes the specific node and all the child nodes from firebase
        // mNewMessageRef.removeValue();

        //Load messages from current date

        mNewMessageRef.child(curDate).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messageList.clear();
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for(DataSnapshot child : children) {
                    ChatboxMessage message = child.getValue(ChatboxMessage.class);
                    if (!messageList.contains(message)) {
                        messageList.add(message);
                       // processMessage(message.getUser(), message);
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
                user = dataSnapshot.child(mAuth.getCurrentUser().getUid()).child("username").getValue().toString();
                rank = dataSnapshot.child(mAuth.getCurrentUser().getUid()).child("rank").getValue().toString();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


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

    private void processMessage(String user, ChatboxMessage message) {
        
        //Notification
        notification.setSmallIcon(R.drawable.ic_person);
        notification.setTicker("New DevChat Message");
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("This is the title");
        notification.setContentText("You have a new message on DevChat");

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(pendingIntent);
        //End Notification

        String messageCur = message.getChatMessage();
        if (messageCur.startsWith("~")) {
            final String[] messageValues = messageCur.split(" ");
            String command = messageValues[0].replace("~", "");
            final String promote_username = messageValues[1];

            //user is admin
            if (rank.equalsIgnoreCase("Admin")) {

                switch (command) {
                    case "ban":

                        break;
                    case "silence":

                        break;
                    case "block":

                        break;
                    case "warn":

                        break;
                    case "promote":
                        mUsers.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                                for (DataSnapshot child : children) {
                                    String user = dataSnapshot.child("username").getValue().toString();
                                    String  rank = dataSnapshot.child("rank").getValue().toString();

                                    if (user.equalsIgnoreCase(promote_username)) {
                                        mUsers.child(mAuth.getCurrentUser().getUid()).child("rank").setValue(messageValues[2]);
                                        ChatboxMessage message = new ChatboxMessage("DevBot", user + " has been promoted to [" + messageValues[2] + "]", messageValues[2], curDate, time);
                                        mDataRef.child(curDate).push().setValue(message);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        break;
                }
            }
            //user is a regular chat user [no admin privilages]
        }else {
            if (!userList.contains(user)) {
                welcomeUser(user);
                userList.add(user);
                mDataRef.child(curDate).push().setValue(message);
            }else {
                mDataRef.child(curDate).push().setValue(message);
            }

        }
       switch (messageCur) {
           case "DevChat Bot is a jerk":
               ChatboxMessage newMessage = new ChatboxMessage("DevBot", user + " Please be nice to me!", "Moderator Bot",curDate, time);
               mDataRef.child(curDate).push().setValue(newMessage);
               break;
       }
    }

    private void welcomeUser(String username) {

        SimpleDateFormat dFormat = new SimpleDateFormat("hh/mm/ss a");
        String curTime = dFormat.format(new Date()).toString();

        ChatboxMessage message = new ChatboxMessage("DevChat Bot", "Welcome to DevChat : " + username, "Moderator Bot", curDate, curTime);
        mDataRef.child(curDate).push().setValue(message);
    }
}

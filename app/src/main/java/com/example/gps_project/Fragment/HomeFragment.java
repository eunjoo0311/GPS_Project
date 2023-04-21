package com.example.gps_project.Fragment;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gps_project.Adapter.PostAdapter;
import com.example.gps_project.model.Post;
import com.example.gps_project.model.User;
import com.example.gps_project.PlaceActivity;
import com.example.gps_project.PostActivity;
import com.example.gps_project.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    ///////////// 변수 /////////////////////////
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postLists;

    private List<String> userList;

    ProgressBar progressBar;

    ImageView btn_Add, btn_Location, exit_button;

    TextView location;

    private String cUser;

    EditText search_post;
////////////////////////////////////////////

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        postLists = new ArrayList<>();
        postAdapter = new PostAdapter(getContext(), postLists);
        recyclerView.setAdapter(postAdapter);

        progressBar = view.findViewById(R.id.progress_circular);

        checkCurrentUserAddress();

        btn_Add = view.findViewById(R.id.btn_Add);
        btn_Add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (location.getText().toString().equals("위치를 설정하세요!")) {
                    Toast.makeText(getActivity().getApplicationContext(), "위치를 먼저 설정해야 합니다", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(getActivity(), PostActivity.class);
                    startActivity(intent);
                }
            }
        });

        location = view.findViewById(R.id.Location);
        btn_Location = view.findViewById(R.id.btn_Location);
        btn_Location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PlaceActivity.class);
                startActivity(intent);
            }
        });
        setLocation();

        search_post = view.findViewById(R.id.search_post);
        search_post.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchPosts(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        final InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
        search_post.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    manager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    search_post.clearFocus();
                    search_post.setFocusable(false);
                    search_post.setFocusableInTouchMode(true);
                    search_post.setFocusable(true);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                    manager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    search_post.clearFocus();
                    search_post.setFocusable(false);
                    search_post.setFocusableInTouchMode(true);
                    search_post.setFocusable(true);
                    return true;
                }
                return false;
            }
        });

        exit_button = view.findViewById(R.id.exit_button);
        exit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search_post.setText(null);
            }
        });

        return view;
    } // onCreateView()

    /*private void checkAllUsers() {
        userList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    userList.add(user.getId());
                }

                readPosts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    } // checkAllUsers()*/

    private void checkCurrentUserAddress() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String userid = firebaseUser.getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user.getNowAddress().equals("")) {
                    cUser = "null";
                } else {
                    cUser = user.getNowAddress();
                }

                readPosts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    } // checkCurrentUserAddress()

    private void readPosts() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postLists.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Post post = snapshot.getValue(Post.class);
                    if (!cUser.equals("null")) {
                        recyclerView.setVisibility(View.VISIBLE);
                        if (post.getNowAddress().equals(cUser)) {  // 현재 사용자의 동네와 게시물의 위치 정보가 일치할 때
                            postLists.add(post);
                        }
                    } else {
                        recyclerView.setVisibility(View.INVISIBLE);
                    }
                }

                postAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    } // readPosts()

    private void setLocation() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String userid = firebaseUser.getUid();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value1 = dataSnapshot.child("thoroughfare").getValue(String.class);
                String value2 = dataSnapshot.child("locality").getValue(String.class);
                if (!value1.equals("") && !value2.equals("")) {
                    location.setText(String.format("%s 이웃(%s)", value2, value1));
                } else {
                    location.setText("위치를 설정하세요!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    } // setLocation()

    private void searchPosts(String s) {
        Query query = FirebaseDatabase.getInstance().getReference("Posts")
                .orderByChild("building")
                .startAt(s)
                .endAt(s + "\uf8ff");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postLists.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Post post = snapshot.getValue(Post.class);
                    if (!cUser.equals("null")) {
                        if (post.getNowAddress().equals(cUser)) {  // 현재 사용자의 동네와 게시물의 위치 정보가 일치할 때
                            postLists.add(post);
                        }
                    }
                }

                postAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

}
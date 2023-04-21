package com.example.gps_project.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gps_project.MainActivity;
import com.example.gps_project.model.Comment;
import com.example.gps_project.model.Post;
import com.example.gps_project.model.User;
import com.example.gps_project.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private Context mContext;
    private List<Comment> mComment;
    private String postid;

    private FirebaseUser firebaseUser;

    public CommentAdapter(Context mContext, List<Comment> mComment, String postid) {
        this.mContext = mContext;
        this.mComment = mComment;
        this.postid = postid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.comment_item, viewGroup, false);
        return new CommentAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        Comment comment = mComment.get(i);

        viewHolder.comment.setText(comment.getComment());
        getUserInfo(viewHolder.image_profile, viewHolder.username, comment.getPublisher(), viewHolder.grade);
        checkPubAddress(comment.getPublisher(), viewHolder.badge);

        /*viewHolder.comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.putExtra("publisherid", comment.getPublisher());
                mContext.startActivity(intent);
            }
        });*/

        viewHolder.image_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.putExtra("publisherid", comment.getPublisher());
                mContext.startActivity(intent);
            }
        });

        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (comment.getPublisher().equals(firebaseUser.getUid())) {

                    AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                    alertDialog.setTitle("댓글 삭제");
                    alertDialog.setMessage("댓글을 완전히 삭제할까요?");
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "취소",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "삭제",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    FirebaseDatabase.getInstance().getReference("댓글")
                                            .child(postid).child(comment.getCommentid())
                                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(mContext, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                    dialogInterface.dismiss();
                                }
                            });
                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#455CDE"));
                            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTypeface(null, Typeface.BOLD);
                        }
                    });
                    alertDialog.show();
                }
                return true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return mComment.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView image_profile, grade, badge;
        public TextView username, comment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image_profile = itemView.findViewById(R.id.image_profile);
            username = itemView.findViewById(R.id.username);
            comment = itemView.findViewById(R.id.comment);
            grade = itemView.findViewById(R.id.grade);
            badge = itemView.findViewById(R.id.badge);
        }
    }

    private void getUserInfo(ImageView imageView, TextView username, String id, ImageView grade) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child(id);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                Glide.with(mContext).load(user.getImageurl()).into(imageView);
                username.setText(user.getUsername());

                int i = user.getLevel();
                if (i < 5) {
                    Glide.with(mContext).load(R.drawable.walking).into(grade);
                } else if (i >= 5 && i < 15) {
                    Glide.with(mContext).load(R.drawable.bike).into(grade);
                } else if (i >= 15 && i < 30) {
                    Glide.with(mContext).load(R.drawable.bus).into(grade);
                } else if (i >= 30 && i < 50) {
                    Glide.with(mContext).load(R.drawable.train).into(grade);
                } else if (i >= 50 && i < 90) {
                    Glide.with(mContext).load(R.drawable.sedan).into(grade);
                } else if (i >= 90) {
                    Glide.with(mContext).load(R.drawable.racing_car).into(grade);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void checkPubAddress(String id, ImageView badge) { // id -> 댓글 쓴 사람의 ID
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postid);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Post post = dataSnapshot.getValue(Post.class);

                DatabaseReference refPlace = FirebaseDatabase.getInstance().getReference("Places");
                refPlace.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChild(id)) {
                            if (!post.getAddress().equals("") && snapshot.child(id).hasChild(post.getAddress())) {
                                if (!snapshot.child(id).child(post.getAddress()).child("address").getValue().toString().equals("")) {
                                    if (snapshot.child(id).child(post.getAddress()).child("address").getValue().toString().equals(post.getAddress())) {
                                        Glide.with(mContext).load(R.drawable.verified).into(badge);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

}
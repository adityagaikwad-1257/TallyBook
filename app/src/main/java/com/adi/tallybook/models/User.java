package com.adi.tallybook.models;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "contacts_table")
public class User implements Serializable {

    @NonNull
    @PrimaryKey
    private String phone_number;

    private String first_name, last_name, display_name
            , user_fid, email, user_image_url;

    /*
        This is not for firebase fireStore
        But, enabling @ContactDatabase to know if the user is an active user of Tally Book or not
     */
    private boolean onTallyBook;

    public User(){
        // required
    }

    @Ignore
    public User(String first_name, @NonNull String phone_number) {
        this.first_name = first_name;
        this.phone_number = phone_number;
    }

    public boolean getOnTallyBook() {
        return onTallyBook;
    }

    public void setOnTallyBook(boolean onTallyBook) {
        this.onTallyBook = onTallyBook;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public void setPhone_number(@NonNull String phone_number) {
        this.phone_number = phone_number;
    }

    @NonNull
    public String getPhone_number() {
        return phone_number;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getUser_fid() {
        return user_fid;
    }

    public void setUser_fid(String user_fid) {
        this.user_fid = user_fid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUser_image_url() {
        return user_image_url;
    }

    public void setUser_image_url(String user_image_url) {
        this.user_image_url = user_image_url;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "phone_number='" + phone_number + '\'' +
                ", first_name='" + first_name + '\'' +
                ", last_name='" + last_name + '\'' +
                ", display_name='" + display_name + '\'' +
                ", user_fid='" + user_fid + '\'' +
                ", email='" + email + '\'' +
                ", user_image_url='" + user_image_url + '\'' +
                ", onTallyBook=" + onTallyBook +
                '}';
    }

    public boolean contentMatches(User user){

        if (!this.phone_number.equals(user.getPhone_number())) return false;

        // checking @first_name
        if (this.first_name != null && !this.first_name.equals(user.first_name)) return false;
        if (user.first_name != null && !user.first_name.equals(this.first_name)) return false;

        // checking @last_name
        if (this.last_name != null && !this.last_name.equals(user.last_name)) return false;
        if (user.last_name != null && !user.last_name.equals(this.last_name)) return false;

         // checking @display_name
        if (this.display_name != null && !this.display_name.equals(user.display_name)) return false;
        if (user.display_name != null && !user.display_name.equals(this.display_name)) return false;

        // checking @user_fid
        if (this.user_fid != null && !this.user_fid.equals(user.user_fid)) return false;
        if (user.user_fid != null && !user.user_fid.equals(this.user_fid)) return false;

        // checking @email
        if (this.email != null && !this.email.equals(user.email)) return false;
        if (user.email != null && !user.email.equals(this.email)) return false;

        // checking @user_image_url
        if (this.user_image_url != null && !this.user_image_url.equals(user.user_image_url)) return false;
        if (user.user_image_url != null && !user.user_image_url.equals(this.user_image_url)) return false;

        return this.onTallyBook == user.onTallyBook;
    }

    public int inviteVisibility(){
        return onTallyBook? View.GONE: View.VISIBLE;
    }

}

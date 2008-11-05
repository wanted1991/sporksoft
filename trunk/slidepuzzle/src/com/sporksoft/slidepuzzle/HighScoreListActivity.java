package com.sporksoft.slidepuzzle;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class HighScoreListActivity extends ListActivity {
    private static final int MENU_CLEAR = 0;
    
    private class DeleteListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int whichButton ) {
            ScoreUtil.getInstance(HighScoreListActivity.this).clearScores();
            initListAdapter();
        }
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.high_score_list_activity);
        initList();
    }

    private void initList() {
        ListView listView = getListView();

        // Visuals
        GradientDrawable grad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xff556688, 0xff66AA22}
              );
        listView.setBackgroundDrawable(grad);
        
        // Add a header to the listView
        View header = getLayoutInflater().inflate(R.layout.high_score_header_item, null);
        listView.addHeaderView(header, null, false);

        initListAdapter();        
    }
    
    private void initListAdapter() {
        long[] times = ScoreUtil.getInstance(this).getAllScores();
        String[] sizes = getResources().getStringArray(R.array.pref_entries_size);
        int len = sizes.length;
        
        ArrayList<ScoreItem> mScores = new ArrayList<ScoreItem>();
        for (int i = 0; i < len; i++) {
            mScores.add(new ScoreItem(sizes[i], times[i]));
        }
        setListAdapter(new HighScoreListAdapter(this, mScores));
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        menu.add(0, MENU_CLEAR, 0, R.string.menu_clear).setIcon(
                R.drawable.ic_menu_delete);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_CLEAR:
                showConfirmDeleteDialog(new DeleteListener());
                break;
        }

        return true;
    }
    
    private void showConfirmDeleteDialog(OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_dialog_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.dialog_yes, listener);
        builder.setNegativeButton(R.string.dialog_no, null);
        builder.setMessage(R.string.delete_dialog_msg);

        builder.show();
    }    
}
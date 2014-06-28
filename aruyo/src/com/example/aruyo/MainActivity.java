package com.example.aruyo;

import com.example.aruyo.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements CamPreView.OnCodeScannedListener{
	private Button startButton;
	private CamPreView preview;
	private static final int MENU_ID_MENU1 = (Menu.FIRST + 1);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startButton = (Button)findViewById(R.id.button1);
		preview = (CamPreView)findViewById(R.id.camPreView1);
		startButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				preview.startCapture(MainActivity.this);
			}
			
		});
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_MENU1, Menu.NONE, "あるよ一覧");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = true;
        switch (item.getItemId()) {
        default:
            ret = super.onOptionsItemSelected(item);
            break;
        case MENU_ID_MENU1:
        	Toast.makeText(this, "あるよ一覧", Toast.LENGTH_LONG).show();
        	ret = true;
            break;
        }
        return ret;
    }

	@Override
	public void onCodeScanned(String scannedCode) {
    	Toast.makeText(this, "あるよ!" + scannedCode, Toast.LENGTH_LONG).show();
		
	}
    
}

package org.jfedor.frozenbubble;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

public class InterstitialActivity extends Activity {
  Button gotoPlayStoreButton;
  Button continueButton;
  ImageView imageView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_interstitial);
    gotoPlayStoreButton = (Button) findViewById(R.id.gotobutton);
    continueButton = (Button) findViewById(R.id.continuebutton);
    imageView = (ImageView) findViewById(R.id.bannerview);
        
    continueButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View arg0) {
        finish();
      }
    });

    OnClickListener gotoPlayStore = new OnClickListener() {
      @Override
      public void onClick(View v) {
        String url = "market://details?id=org.jfedor.beaver&referrer=utm_source%3Dfrozenbubble";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
      }
    };

    gotoPlayStoreButton.setOnClickListener(gotoPlayStore);
    imageView.setOnClickListener(gotoPlayStore);
  }
}

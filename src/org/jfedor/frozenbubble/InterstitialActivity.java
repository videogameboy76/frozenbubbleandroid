package org.jfedor.frozenbubble;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

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
        Intent i = new Intent(Intent.ACTION_VIEW,
            Uri.parse("market://details?id=org.jfedor.beaver&referrer=utm_source%3Dfrozenbubble"));
        try {
          startActivity(i);
        } catch (ActivityNotFoundException anfe1) {
          i = new Intent(Intent.ACTION_VIEW,
              Uri.parse("market://search?q=busy beaver"));
          try {
            startActivity(i);
          } catch (ActivityNotFoundException anfe3) {
            Toast.makeText(getApplicationContext(), 
                "Could not access market.  Are you connected to the internet?",
                Toast.LENGTH_SHORT).show();
          }
        }
      }
    };

    gotoPlayStoreButton.setOnClickListener(gotoPlayStore);
    imageView.setOnClickListener(gotoPlayStore);
  }
}

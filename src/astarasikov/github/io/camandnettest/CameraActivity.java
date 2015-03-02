package astarasikov.github.io.camandnettest;

import android.app.Activity;
import android.os.Bundle;


public class CameraActivity extends Activity {

    private MyCameraView mMyCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mMyCameraView = new MyCameraView(this));
    }
}

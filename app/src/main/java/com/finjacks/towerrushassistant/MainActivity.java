package com.finjacks.towerrushassistant;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(28, 36, 28, 28);
        page.setBackgroundColor(Color.rgb(15, 18, 27));

        TextView title = new TextView(this);
        title.setText("TOWER RUSH 2");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 8);
        page.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("ASSISTED PLAYER  •  GALAXSYS");
        subtitle.setTextColor(Color.rgb(112, 190, 255));
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 32);
        page.addView(subtitle, matchWrap());

        status = new TextView(this);
        status.setText("READY TO SET UP\n\nScreen analysis is not connected yet. The next build stage will add Android's screen-capture permission flow.");
        status.setTextColor(Color.rgb(225, 230, 240));
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(20, 24, 20, 24);
        status.setBackgroundColor(Color.rgb(29, 35, 48));
        page.addView(status, matchWrap());

        Button setup = new Button(this);
        setup.setText("SCREEN ANALYSIS — COMING NEXT");
        setup.setAllCaps(false);
        setup.setEnabled(false);
        LinearLayout.LayoutParams buttonParams = matchWrap();
        buttonParams.topMargin = 28;
        page.addView(setup, buttonParams);

        TextView safety = new TextView(this);
        safety.setText("You stay in control. This first version does not place bets, tap game controls, predict random outcomes, or connect to your 1win account. Never treat an estimate as a guaranteed win.");
        safety.setTextColor(Color.rgb(170, 180, 195));
        safety.setTextSize(13);
        safety.setGravity(Gravity.CENTER);
        safety.setPadding(8, 32, 8, 8);
        page.addView(safety, matchWrap());

        setContentView(page);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}

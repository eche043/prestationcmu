package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.text.SpannableString;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;

public class OnboardingActivity extends AppCompatActivity {

    // Vos déclarations originales
    private ImageView ivLogo;
    private TextView tvTitle, tvSubtitle;
    private ConstraintLayout stepContainer1, stepContainer2, stepContainer3;
    private Button btnUnderstand;
    private ConstraintLayout layout;

    // Vos délais ORIGINAUX conservés
    private static final int LOGO_DELAY = 300;
    private static final int LOGO_MOVE_DELAY = 1500;
    private static final int TITLE_DELAY = 2000;
    private static final int TITLE_MOVE_DELAY = 3500;
    private static final int SUBTITLE_DELAY = 4300;
    private static final int SUBTITLE_MOVE_DELAY = 5500;
    private static final int STEP1_DELAY = 6300;
    private static final int STEP2_DELAY = 7300;
    private static final int STEP3_DELAY = 8300;
    private static final int BUTTON_DELAY = 10000;


    private static final float LOGO_FINAL_POSITION = 0.05f;
    private static final float TITLE_FINAL_POSITION = 0.20f;
    private static final float SUBTITLE_FINAL_POSITION = 0.30f;

    private final int REQUEST_PHONE_STATE_PERMISSION =1;
    private UtilsInfosAppareil utilsInfos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);


        layout = findViewById(R.id.activity_onboarding_layout);
        ivLogo = findViewById(R.id.ivLogo);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        stepContainer1 = findViewById(R.id.stepContainer1);
        stepContainer2 = findViewById(R.id.stepContainer2);
        stepContainer3 = findViewById(R.id.stepContainer3);
        btnUnderstand = findViewById(R.id.btnUnderstand);

        requestStatePermission();

        utilsInfos = new UtilsInfosAppareil(this);
        utilsInfos.obtenirInformationsSysteme(this);



        initStepPositions();




        btnUnderstand.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity.this, ChoixEtablissementActivity.class));
            finish();
        });

        startAnimationSequence();
    }

    private void requestStatePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("OnboardingActivity", "DEMANDE DE PERMISSION");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_PHONE_STATE_PERMISSION);
        } else {
            // Permission déjà accordée, obtenez les infos
            Log.d("OnboardingActivity", "PERMISSION DEJA ACCORDEE");
            // Utilisez les infos...
        }
    }

    private void initStepPositions() {
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.setVerticalBias(R.id.stepContainer1, 1.0f);
        set.setVerticalBias(R.id.stepContainer2, 1.0f);
        set.setVerticalBias(R.id.stepContainer3, 1.0f);
        set.applyTo(layout);
    }

    private void startAnimationSequence() {
        final Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(800);


        new Handler().postDelayed(() -> {
            ivLogo.setVisibility(View.VISIBLE);
            ivLogo.setAlpha(1f);
            ivLogo.startAnimation(fadeIn);
        }, LOGO_DELAY);


        new Handler().postDelayed(() -> moveViewToPosition(ivLogo, LOGO_FINAL_POSITION), LOGO_MOVE_DELAY);


        new Handler().postDelayed(() -> {
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setAlpha(1f);
            tvTitle.startAnimation(fadeIn);
        }, TITLE_DELAY);

        new Handler().postDelayed(() -> moveViewToPosition(tvTitle, TITLE_FINAL_POSITION), TITLE_MOVE_DELAY);


        new Handler().postDelayed(() -> {
            tvSubtitle.setVisibility(View.VISIBLE);
            tvSubtitle.setAlpha(1f);
            tvSubtitle.startAnimation(fadeIn);
        }, SUBTITLE_DELAY);


        new Handler().postDelayed(() -> moveViewToPosition(tvSubtitle, SUBTITLE_FINAL_POSITION), SUBTITLE_MOVE_DELAY);


        new Handler().postDelayed(() -> animateStepFromBottom(stepContainer1, 0.40f), STEP1_DELAY);
        new Handler().postDelayed(() -> animateStepFromBottom(stepContainer2, 0.50f), STEP2_DELAY);
        new Handler().postDelayed(() -> animateStepFromBottom(stepContainer3, 0.60f), STEP3_DELAY);


        new Handler().postDelayed(() -> {
            btnUnderstand.setVisibility(View.VISIBLE);
            btnUnderstand.setAlpha(0f);
            btnUnderstand.setTranslationY(200f);
            btnUnderstand.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .start();
        }, BUTTON_DELAY);
    }


    private void moveViewToPosition(View view, float verticalBias) {
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.setVerticalBias(view.getId(), verticalBias);
        set.applyTo(layout);

        view.animate().setDuration(600).start();
    }


    private void animateStepFromBottom(View view, float targetBias) {
        view.setVisibility(View.VISIBLE);

        ValueAnimator biasAnimator = ValueAnimator.ofFloat(1.0f, targetBias);
        biasAnimator.addUpdateListener(animation -> {
            float bias = (float) animation.getAnimatedValue();
            ConstraintSet set = new ConstraintSet();
            set.clone(layout);
            set.setVerticalBias(view.getId(), bias);
            set.applyTo(layout);
        });

        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(biasAnimator, alphaAnimator);
        animatorSet.setDuration(600);
        animatorSet.start();
    }
}
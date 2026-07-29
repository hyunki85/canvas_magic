package com.h2play.canvas_magic.features.tutorial;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.util.Analytics;

public class TutorialDialogueActivity extends AppCompatActivity {

    public static final String EXTRA_PHASE = "tutorial_phase";
    public static final int PHASE_PRE_DRAW = 1;
    public static final int PHASE_PRE_PIN = 2;
    public static final int PHASE_QUIZ = 3;
    public static final int PHASE_PRACTICE = 4;

    private static final int CORRECT_ANSWER = 9;

    private TextView tvDialogue;
    private TextView tvCharacterName;
    private ImageView ivAvatar;
    private View tapIndicator;
    private GridLayout quizGrid;

    private String[] dialogues;
    private int currentStep = 0;
    private boolean isTyping = false;
    private boolean isQuizMode = false;
    private String currentFullText = "";
    private int currentPhase = PHASE_PRE_DRAW;
    private boolean finishedNormally = false;
    private final Handler typewriterHandler = new Handler(Looper.getMainLooper());

    public static Intent getStartIntent(Context context, int phase) {
        Intent intent = new Intent(context, TutorialDialogueActivity.class);
        intent.putExtra(EXTRA_PHASE, phase);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_dialogue);

        tvDialogue = findViewById(R.id.tv_dialogue);
        tvCharacterName = findViewById(R.id.tv_character_name);
        ivAvatar = findViewById(R.id.iv_avatar);
        tapIndicator = findViewById(R.id.tap_indicator);
        quizGrid = findViewById(R.id.quiz_grid);

        // 탭 인디케이터 깜빡임 애니메이션
        ObjectAnimator blink = ObjectAnimator.ofFloat(tapIndicator, "alpha", 1f, 0.2f);
        blink.setDuration(600);
        blink.setRepeatMode(ValueAnimator.REVERSE);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.setInterpolator(new LinearInterpolator());
        blink.start();

        currentPhase = getIntent().getIntExtra(EXTRA_PHASE, PHASE_PRE_DRAW);
        int phase = currentPhase;
        initDialogues(phase);

        if (phase == PHASE_QUIZ) {
            showQuiz();
        } else {
            showStep(0);
            findViewById(R.id.root_layout).setOnClickListener(v -> onTap());
        }
    }

    private void initDialogues(int phase) {
        if (phase == PHASE_PRE_DRAW) {
            dialogues = new String[]{
                    getString(R.string.tutorial_vn_step1),
                    getString(R.string.tutorial_vn_step2),
                    getString(R.string.tutorial_vn_step3),
                    getString(R.string.tutorial_vn_step4),
            };
        } else if (phase == PHASE_PRE_PIN) {
            dialogues = new String[]{
                    getString(R.string.tutorial_vn_step5),
                    getString(R.string.tutorial_vn_step6),
            };
        } else if (phase == PHASE_PRACTICE) {
            dialogues = new String[]{
                    getString(R.string.tutorial_practice_intro),
            };
        } else {
            dialogues = new String[]{};
        }
    }

    private void showQuiz() {
        isQuizMode = true;
        tapIndicator.setVisibility(View.GONE);

        // 질문 표시
        String question = getString(R.string.tutorial_quiz_question);
        startTypewriter(question);

        // 숫자 그리드 표시
        quizGrid.setVisibility(View.VISIBLE);
        quizGrid.removeAllViews();

        for (int i = 0; i < 10; i++) {
            final int number = i;
            MaterialButton btn = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(String.valueOf(i));
            btn.setTextSize(18);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 5, 1f);
            params.rowSpec = GridLayout.spec(i / 5);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> onQuizAnswer(number));
            quizGrid.addView(btn);
        }
    }

    private void onQuizAnswer(int number) {
        Bundle qb = new Bundle();
        qb.putLong("answer", number);
        qb.putString("result", number == CORRECT_ANSWER ? "correct" : "wrong");
        Analytics.log(this, Analytics.TUTORIAL_QUIZ, qb);

        if (number == CORRECT_ANSWER) {
            // 정답
            typewriterHandler.removeCallbacksAndMessages(null);
            tvDialogue.setText(getString(R.string.tutorial_quiz_correct));
            quizGrid.setVisibility(View.GONE);

            // 잠시 후 종료
            typewriterHandler.postDelayed(() -> {
                finishedNormally = true;
                setResult(RESULT_OK);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 1500);
        } else {
            // 오답
            Toast.makeText(this, R.string.tutorial_quiz_wrong, Toast.LENGTH_SHORT).show();
        }
    }

    private void showStep(int step) {
        if (step >= dialogues.length) {
            finishedNormally = true;
            setResult(RESULT_OK);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }
        currentStep = step;
        currentFullText = dialogues[step];

        // 대사 한 줄마다 기록해야 "몇 번째 문장에서 나갔는지"를 볼 수 있다
        Bundle b = new Bundle();
        b.putLong("phase", currentPhase);
        b.putLong("step", step);
        b.putLong("total_steps", dialogues.length);
        Analytics.log(this, Analytics.TUTORIAL_STEP, b);

        startTypewriter(currentFullText);
    }

    private void startTypewriter(String text) {
        isTyping = true;
        if (!isQuizMode) tapIndicator.setVisibility(View.INVISIBLE);
        tvDialogue.setText("");
        currentFullText = text;
        final int[] charIndex = {0};
        typewriterHandler.removeCallbacksAndMessages(null);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (charIndex[0] <= text.length()) {
                    tvDialogue.setText(text.substring(0, charIndex[0]));
                    charIndex[0]++;
                    typewriterHandler.postDelayed(this, 40);
                } else {
                    isTyping = false;
                    if (!isQuizMode) tapIndicator.setVisibility(View.VISIBLE);
                }
            }
        };
        typewriterHandler.post(runnable);
    }

    private void onTap() {
        if (isQuizMode) return;
        if (isTyping) {
            typewriterHandler.removeCallbacksAndMessages(null);
            tvDialogue.setText(currentFullText);
            isTyping = false;
            tapIndicator.setVisibility(View.VISIBLE);
        } else {
            showStep(currentStep + 1);
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        // 정상 종료가 아니면 = 뒤로가기 등으로 중간 이탈. 어느 단계에서 나갔는지가 핵심 지표다.
        if (!finishedNormally && isFinishing()) {
            Bundle b = new Bundle();
            b.putLong("phase", currentPhase);
            b.putLong("step", currentStep);
            b.putString("quiz_mode", String.valueOf(isQuizMode));
            Analytics.log(this, Analytics.TUTORIAL_EXIT, b);
        }
        typewriterHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

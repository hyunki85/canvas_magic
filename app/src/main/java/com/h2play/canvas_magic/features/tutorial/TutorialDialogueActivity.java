package com.h2play.canvas_magic.features.tutorial;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.GridLayout;
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
    private View tapIndicator;
    private GridLayout quizGrid;

    private String[] dialogues;
    private int currentStep = 0;
    private boolean isQuizMode = false;
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
        tapIndicator = findViewById(R.id.tap_indicator);
        quizGrid = findViewById(R.id.quiz_grid);


        currentPhase = getIntent().getIntExtra(EXTRA_PHASE, PHASE_PRE_DRAW);
        int phase = currentPhase;
        ((TextView) findViewById(R.id.tutorial_heading)).setText(phase == PHASE_PRE_DRAW
                ? R.string.tutorial_intro_title : R.string.tutorial_pin_heading);
        initDialogues(phase);

        if (phase == PHASE_QUIZ) {
            showQuiz();
        } else {
            showStep(savedInstanceState == null ? 0 : savedInstanceState.getInt("step", 0));
            tapIndicator.setOnClickListener(v -> showStep(currentStep + 1));
        }
    }

    private void initDialogues(int phase) {
        if (phase == PHASE_PRE_DRAW) {
            dialogues = new String[]{
                    getString(R.string.tutorial_intro_pick),
                    getString(R.string.tutorial_intro_tools),
            };
        } else if (phase == PHASE_PRE_PIN) {
            dialogues = new String[]{
                    getString(R.string.tutorial_pin_instruction),
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
        showDialogue(question);

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
        tvCharacterName.setText(currentPhase == PHASE_PRE_DRAW
                ? getString(R.string.tutorial_intro_progress, step + 1, dialogues.length)
                : getString(R.string.tutorial_reveal_title));
        ((MaterialButton) tapIndicator).setText(step == dialogues.length - 1
                ? R.string.tutorial_start_practice : R.string.tutorial_continue);

        // 대사 한 줄마다 기록해야 "몇 번째 문장에서 나갔는지"를 볼 수 있다
        Bundle b = new Bundle();
        b.putLong("phase", currentPhase);
        b.putLong("step", step);
        b.putLong("total_steps", dialogues.length);
        Analytics.log(this, Analytics.TUTORIAL_STEP, b);

        showDialogue(dialogues[step]);
    }

    private void showDialogue(String text) {
        tvDialogue.setText(text);
        if (!isQuizMode) tapIndicator.setVisibility(View.VISIBLE);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("step", currentStep);
        super.onSaveInstanceState(state);
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

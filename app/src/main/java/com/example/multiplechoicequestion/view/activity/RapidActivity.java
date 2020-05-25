package com.example.multiplechoicequestion.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.multiplechoicequestion.R;
import com.example.multiplechoicequestion.room.CategoricalQuestion;
import com.example.multiplechoicequestion.room.Question;
import com.example.multiplechoicequestion.view.viewmodel.QuestionViewModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RapidActivity extends AppCompatActivity {
    private QuestionViewModel mViewModel;

    public static final String EXTRA_SCORE ="extraScore";
    public static final String CATEGORY_ID = "categoryId";
    public static final String SET_NR = "setNr";
    private static final long COUNTDOWN_IN_MILLIS = 30000;

    private TextView tvQuestion;
    private TextView tvScore;
    private TextView tvQuestionCount;
    private TextView tvTimer;
    private RadioGroup rbGroup;
    private RadioButton rb1;
    private RadioButton rb2;
    private RadioButton rb3;
    private RadioButton rb4;
    private Button buttonConfirmNext;

    private ColorStateList textColorDefaultRb;
    private ColorStateList textColorDefaultCd;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;

    private List<Question> questionList;
    private int questionCounter;
    private int questionCountTotal;
    private Question currentQuestion;

    private int score;
    private boolean answered;

    private long backPressedTime;
    private int categoryIndex;
    private int setNr;

    private InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rapid);

        categoryIndex = getIntent().getIntExtra(CATEGORY_ID, 0);
        setNr = getIntent().getIntExtra(SET_NR, 1);

        tvQuestion = findViewById(R.id.question_text);
        tvScore = findViewById(R.id.score);
        tvQuestionCount = findViewById(R.id.question_number);
        tvTimer = findViewById(R.id.tv_countdown);
        rbGroup = findViewById(R.id.radio_group);
        rb1 = findViewById(R.id.radio_button1);
        rb2 = findViewById(R.id.radio_button2);
        rb3 = findViewById(R.id.radio_button3);
        rb4 = findViewById(R.id.radio_button4);
        buttonConfirmNext = findViewById(R.id.submit_area);

        textColorDefaultRb = rb1.getTextColors();
        textColorDefaultCd = tvTimer.getTextColors();

        mViewModel = new ViewModelProvider(this).get(QuestionViewModel.class);
        //questionList = mViewModel.getAllWords().getValue();

        System.out.println();

        mViewModel.getAllCategoricalQuestions().observe(this, new Observer<List<CategoricalQuestion>>() {
            @Override
            public void onChanged(List<CategoricalQuestion> questions) {
                //adapater.setWords(questions);
                questionList = questions.get(categoryIndex).questionList;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    questionList = questionList.stream()
                            .filter(q -> q.getSetNr() == setNr).collect(Collectors.toList());
                } else {
                    List<Question> newQuestionList = new ArrayList<>();
                    for (Question question : questionList) {
                        if (question.getSetNr() == setNr)
                            newQuestionList.add(question);
                    }
                    questionList = newQuestionList;
                }
                questionCountTotal = questionList.size();


                Collections.shuffle(questionList);

                showNextQuestion();
            }
        });


        buttonConfirmNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!answered){
                    if(rb1.isChecked() || rb2.isChecked() || rb3.isChecked() || rb4.isChecked()){
                        checkAnswer();
                    }else{
                        Toast.makeText(RapidActivity.this,"Please press a button",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    showNextQuestion();
                }
            }
        });

        //interstetialAd

        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstetial_ad_unit_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        interstitialAd.loadAd(adRequest);


    }

    private void showNextQuestion(){
        rb1.setTextColor(textColorDefaultRb);
        rb2.setTextColor(textColorDefaultRb);
        rb3.setTextColor(textColorDefaultRb);
        rb4.setTextColor(textColorDefaultRb);
        rbGroup.clearCheck();

        if(questionCounter < questionCountTotal){
            currentQuestion = questionList.get(questionCounter);
            tvQuestion.setText(currentQuestion.getQuestion());
            rb1.setText(currentQuestion.getOption1());
            rb2.setText(currentQuestion.getOption2());
            rb3.setText(currentQuestion.getOption3());
            rb4.setText(currentQuestion.getOption4());

            questionCounter++;
            tvQuestionCount.setText("Question: "+ questionCounter + "/"  + questionCountTotal);
            answered= false;
            buttonConfirmNext.setText("Submit");

            timeLeftInMillis = COUNTDOWN_IN_MILLIS;
            startCountDown();
        }else{
            finishQuiz();
        }
    }
    private void startCountDown(){
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountDownText();
                checkAnswer();
            }
        }.start();
    }
    private void updateCountDownText(){
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted = String.format(Locale.getDefault(),"%02d:%02d", minutes, seconds);

        tvTimer.setText(timeFormatted);

        if (timeLeftInMillis < 10000){
            tvTimer.setTextColor(Color.RED);
        }else{
            tvTimer.setTextColor(textColorDefaultCd);
        }

    }
    private void checkAnswer(){
        answered = true;

        countDownTimer.cancel();

        RadioButton rbSelected = findViewById(rbGroup.getCheckedRadioButtonId());
        int answerNr = rbGroup.indexOfChild(rbSelected) + 1;

        if(answerNr == currentQuestion.getAnswerNr()){
            score++;
            tvScore.setText("Score: "+ score);
        }
        showSolution();
    }

    private void showSolution(){
        rb1.setTextColor(Color.RED);
        rb2.setTextColor(Color.RED);
        rb3.setTextColor(Color.RED);
        rb4.setTextColor(Color.RED);

        switch (currentQuestion.getAnswerNr()){
            case 1:
                rb1.setTextColor(Color.GREEN);
                tvQuestion.setText("Answer 1 is correct");
                break;

            case 2:
                rb2.setTextColor(Color.GREEN);
                tvQuestion.setText("Answer 2 is correct");
                break;

            case 3:
                rb3.setTextColor(Color.GREEN);
                tvQuestion.setText("Answer 3 is correct");
                break;

            case 4:
                rb4.setTextColor(Color.GREEN);
                tvQuestion.setText("Answer 4 is correct");
                break;
        }

        if(questionCounter < questionCountTotal){
            buttonConfirmNext.setText("Next");
        }else{
            buttonConfirmNext.setText("Finish");
        }

    }

    private void finishQuiz(){
        interstitialAd.show();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SCORE,score);
        setResult(RESULT_OK, resultIntent);
        finish();

    }

    @Override
    public void onBackPressed() {
        if(backPressedTime+2000 > System.currentTimeMillis()){
            finishQuiz();
        }else{
            Toast.makeText(RapidActivity.this, "Press back again to finish", Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(countDownTimer != null){
            countDownTimer.cancel();
        }
    }
}

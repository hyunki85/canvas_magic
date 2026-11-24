package com.h2play.canvas_magic.features.menu;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.h2play.canvas_magic.BuildConfig;
import com.h2play.canvas_magic.R;
import com.h2play.canvas_magic.data.model.PackRef;
import com.h2play.canvas_magic.data.model.TutorialProgress;
import com.h2play.canvas_magic.data.model.response.ShapeInfo;
import com.h2play.canvas_magic.features.base.BaseActivity;
import com.h2play.canvas_magic.features.common.ErrorView;
import com.h2play.canvas_magic.features.list.ShapeListActivity;
import com.h2play.canvas_magic.features.main.MainActivity;
import com.h2play.canvas_magic.features.share.ShareActivity;
import com.h2play.canvas_magic.injection.component.ActivityComponent;
import com.h2play.canvas_magic.util.ads.BannerAdController;
import com.h2play.canvas_magic.util.ads.ExitAdController;
import com.h2play.canvas_magic.util.Analytics;
import com.h2play.canvas_magic.util.PromoImpressionTracker;
import com.h2play.canvas_magic.util.HomeExperiencePreferences;
import com.h2play.canvas_magic.util.NotificationPermissionManager;
import com.h2play.canvas_magic.util.ReminderScheduler;
import com.h2play.canvas_magic.util.TopicManager;
import com.h2play.canvas_magic.util.CommunityDeepLink;
import com.h2play.canvas_magic.util.DrawingExperience;
import com.h2play.canvas_magic.util.DrawingCatalog;
import com.h2play.canvas_magic.features.packs.DrawingPackActivity;
import com.vorlonsoft.android.rate.AppRate;

import com.h2play.canvas_magic.features.menu.config.MenuConfig;
import com.h2play.canvas_magic.features.menu.config.MenuConfigLoader;
import com.h2play.canvas_magic.features.menu.config.MenuItemConfig;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class MenuActivity extends BaseActivity implements MenuMvpView, ErrorView.ErrorListener {

    public static final String EXTRA_DESTINATION = "destination";
    public static final String DESTINATION_COMMUNITY = "community";
    public static final String DESTINATION_SHAPE_LIST = "shape_list";
    public static final String DESTINATION_DRAWING_PACK = "drawing_pack";
    private static final String STATE_SHOW_INTRO = "show_home_intro";
    private static final String STATE_PROMO_IMPRESSIONS = "promo_impressions";

    @Inject
    MenuPresenter menuPresenter;

    private ErrorView errorView;
    private ProgressBar progressBar;
    private View startButton;
    private TextView tvStartLabel;
    private View moreButton;
    private View btnChannel;
    private View btnHelp;
    private View imgRate;
    private View imgShare;
    private View btnShare;
    private View heroSection;
    private View heroIntroContent;
    private View returningKicker;
    private View choosePackButton;
    private View[] sparkleViews;
    private boolean returningUser;
    private boolean introAnimationsStarted;
    private boolean showHomeIntro;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private boolean notificationOptInShowing;

    // Dynamic menu
    private RecyclerView rvDynamicMenu;
    private NestedScrollView scrollView;
    private DynamicMenuAdapter dynamicAdapter;
    private int activeMenuConfigVersion;
    private String activeMenuExperiment = "";
    private final PromoImpressionTracker promoImpressions = new PromoImpressionTracker();
    private boolean homeResumed;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signInAnonymously();
        }
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                        } else {
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<String> impressions = savedInstanceState.getStringArrayList(STATE_PROMO_IMPRESSIONS);
            promoImpressions.restore(impressions);
        }

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    NotificationPermissionManager.setOptedIn(this, granted);
                    TopicManager.updateTopics(this);
                    Analytics.log(this, Analytics.NOTIFICATION_PERMISSION_RESULT,
                            "result", granted ? "granted" : "denied");
                });

        // Making sure Firebase is initialized before accessing FirebaseAuth
        if (!com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            mAuth = FirebaseAuth.getInstance();
        } else {
            com.google.firebase.FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
        }

        errorView = findViewById(R.id.view_error);
        progressBar = findViewById(R.id.progress);
        startButton = findViewById(R.id.btn_start);
        tvStartLabel = findViewById(R.id.tv_start_label);
        moreButton = findViewById(R.id.btn_more);
        btnChannel = findViewById(R.id.btn_channel);
        btnHelp = findViewById(R.id.btn_help);
        imgRate = findViewById(R.id.img_rate);
        imgShare = findViewById(R.id.img_share);
        btnShare = findViewById(R.id.btn_share);
        heroSection = findViewById(R.id.hero_section);
        heroIntroContent = findViewById(R.id.hero_intro_content);
        returningKicker = findViewById(R.id.tv_returning_kicker);
        choosePackButton = findViewById(R.id.btn_choose_pack);
        sparkleViews = new View[] {
                findViewById(R.id.sparkle1),
                findViewById(R.id.sparkle2),
                findViewById(R.id.sparkle3)
        };
        Button btnRestartTutorial = findViewById(R.id.btn_restart_tutorial);
        rvDynamicMenu = findViewById(R.id.rv_dynamic_menu);
        scrollView = findViewById(R.id.scroll_view);
        showHomeIntro = savedInstanceState == null
                ? !HomeExperiencePreferences.hasSeenIntro(this)
                : savedInstanceState.getBoolean(STATE_SHOW_INTRO, false);
        HomeExperiencePreferences.markIntroSeen(this);

        btnChannel.setOnClickListener(v -> onChannelClick());
        btnHelp.setOnClickListener(v -> onHelpClick());
        moreButton.setOnClickListener(v -> onMoreClick());
        imgRate.setOnClickListener(v -> onRateClick());
        imgShare.setOnClickListener(v -> onShareLinkClick());
        startButton.setOnClickListener(v -> onStartClick());
        choosePackButton.setOnClickListener(v -> menuPresenter.getShapeList());
        btnShare.setOnClickListener(v -> onShareClick());
        btnRestartTutorial.setOnClickListener(v -> onRestartTutorialClick());
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (view, scrollX, scrollY, oldScrollX, oldScrollY) -> trackVisiblePromoImpressions());

        // Try load dynamic menu from Remote Config first, then assets fallback
        setupDynamicMenu();

        errorView.setErrorListener(this);

        new BannerAdController(this, findViewById(R.id.adView), BannerAdController.HOME,
                findViewById(R.id.ad_privacy_options));
        ExitAdController exitAd = new ExitAdController(this, ExitAdController.Placement.MENU);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                exitAd.onBack(MenuActivity.this::finish);
            }
        });

        AppRate.with(this)
                .setInstallDays((byte) 0)
                .setLaunchTimes((byte) 3)
                .setRemindInterval((byte) 1)
                .setRemindLaunchesNumber((byte) 1)
                .monitor();
        // 실제 마술을 충분히 경험하기 전에는 평점 요청으로 흐름을 끊지 않는다.
        if (menuPresenter.getTrickCount() >= 3) {
            AppRate.showRateDialogIfMeetsConditions(this);
        }

        heroSection.post(() -> handleNavigationIntent(getIntent()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        homeResumed = true;
        rvDynamicMenu.post(this::trackVisiblePromoImpressions);
        if (!menuPresenter.hasPersistedPackRef()) {
            String legacyBuiltIn = DrawingExperience.lastPack(this);
            if (DrawingCatalog.index(legacyBuiltIn) >= 0) {
                menuPresenter.rememberBuiltInPack(legacyBuiltIn);
            }
        }
        updateHomeForVisitState();
        tvStartLabel.setText(R.string.start_trick);
        TextView activePack = findViewById(R.id.tv_active_pack);
        activePack.setVisibility(View.GONE);
        if (returningUser) {
            PackRef ref = menuPresenter.getLastPackRef();
            int builtIn = ref != null && ref.kind == PackRef.Kind.BUILT_IN
                    ? DrawingCatalog.index(ref.id) : -1;
            ShapeInfo lastShape = ref != null && ref.kind == PackRef.Kind.CUSTOM
                    ? menuPresenter.getLastShape() : null;
            if (builtIn >= 0) {
                activePack.setText(getString(R.string.active_drawing_set,
                        getResources().getStringArray(R.array.drawing_pack_titles)[builtIn]));
            } else if (lastShape != null) {
                activePack.setText(getString(R.string.active_drawing_set, lastShape.name));
            }
            if (builtIn >= 0 || lastShape != null) {
                activePack.setVisibility(View.VISIBLE);
                tvStartLabel.setText(R.string.perform_now);
            }
        }
        menuPresenter.checkNeedGuidE();
        ReminderScheduler.cancelRetiredReminders(this);
        TopicManager.updateTopics(this);
        heroSection.post(this::maybeShowNotificationOptIn);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_SHOW_INTRO, showHomeIntro);
        outState.putStringArrayList(STATE_PROMO_IMPRESSIONS, promoImpressions.snapshot());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        homeResumed = false;
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && rvDynamicMenu != null) {
            rvDynamicMenu.post(this::trackVisiblePromoImpressions);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
    }

    @Override
    public int getLayout() {
        return R.layout.activity_menu;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        menuPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        menuPresenter.detachView();
    }

    public void onChannelClick() {
        Intent intent = new Intent(this, ShapeListActivity.class);
        startActivity(intent);
    }

    public void onHelpClick() {
        // 튜토리얼 전용 웹 페이지로 이동 (로컬 에셋)
        String language = getResources().getConfiguration().getLocales().get(0).getLanguage();
        String tutorialAsset = "tutorial.html";
        if ("ko".equals(language)) {
            tutorialAsset = "tutorial_ko.html";
        } else if ("ru".equals(language)) {
            tutorialAsset = "tutorial_ru.html";
        }

        Intent intent = new Intent(this, com.h2play.canvas_magic.features.web.WebViewActivity.class);
        PackRef active = menuPresenter.getLastPackRef();
        String pack = active != null && active.kind == PackRef.Kind.BUILT_IN ? active.id : null;
        String contextQuery = DrawingCatalog.index(pack) >= 0 ? "?pack=" + Uri.encode(pack) : "";
        intent.putExtra("url", "file:///android_asset/" + tutorialAsset + contextQuery);
        intent.putExtra("title", getString(R.string.drawing_help_title));
        startActivity(intent);
    }

    public void onMoreClick() {
        String url = "https://play.google.com/store/apps/dev?id=8030976532724501230";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    public void onRateClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
        intent.setPackage("com.android.vending");
        startActivity(intent);
    }

    public void onShareLinkClick() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
            String shareMessage = "https://play.google.com/store/apps/details?id=" + "com.h2play.canvas_magic";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_share_app)));
        } catch (Exception e) {
            // Handle exception
        }
    }

    public void onStartClick() {
        if (menuPresenter.isGuideNeeded()) {
            startTutorial();
            return;
        }
        PackRef pack = menuPresenter.getLastPackRef();
        if (returningUser && pack != null) {
            Bundle params = new Bundle();
            params.putString("action", "resume_pack");
            params.putString("pack_kind", pack.kind == PackRef.Kind.BUILT_IN ? "built_in" : "custom");
            Analytics.log(this, Analytics.MENU_ACTION, params);
            startPack(pack, "home_resume");
            return;
        }
        menuPresenter.getShapeList();
    }

    private void startPack(PackRef pack, String entryPoint) {
        if (pack.kind == PackRef.Kind.BUILT_IN) {
            menuPresenter.rememberBuiltInPack(pack.id);
            DrawingExperience.rememberPack(this, pack.id);
            startActivity(MainActivity.getStartIntent(this, 0)
                    .putExtra(DrawingExperience.EXTRA_PACK, pack.id)
                    .putExtra(MainActivity.EXTRA_ENTRY_POINT, entryPoint));
        } else {
            menuPresenter.rememberCustomPack(pack.id);
            DrawingExperience.rememberPack(this, null);
            startActivity(MainActivity.getCustomPackIntent(this, pack.id, entryPoint));
        }
    }

    private void updateHomeForVisitState() {
        returningUser = menuPresenter.getTrickCount() > 0 && !menuPresenter.isGuideNeeded();
        boolean expandedIntro = showHomeIntro && menuPresenter.getTrickCount() == 0;

        heroIntroContent.setVisibility(expandedIntro ? View.VISIBLE : View.GONE);
        returningKicker.setVisibility(expandedIntro ? View.GONE : View.VISIBLE);
        ((TextView) returningKicker).setText(returningUser
                ? R.string.returning_user_kicker : R.string.menu_subtitle);
        choosePackButton.setVisibility(returningUser ? View.VISIBLE : View.GONE);

        int verticalPadding = dpToPx(expandedIntro ? 40 : 16);
        heroSection.setPadding(
                heroSection.getPaddingLeft(),
                verticalPadding,
                heroSection.getPaddingRight(),
                dpToPx(expandedIntro ? 28 : 8));

        ViewGroup.MarginLayoutParams startParams =
                (ViewGroup.MarginLayoutParams) startButton.getLayoutParams();
        startParams.topMargin = dpToPx(expandedIntro ? 24 : 0);
        startButton.setLayoutParams(startParams);

        if (!expandedIntro) {
            startButton.clearAnimation();
            for (View sparkle : sparkleViews) {
                sparkle.animate().cancel();
                sparkle.setVisibility(View.GONE);
            }
            heroSection.setAlpha(1f);
        } else {
            for (View sparkle : sparkleViews) {
                sparkle.setVisibility(View.VISIBLE);
            }
            playIntroAnimationsOnce();
        }
    }

    private void maybeShowNotificationOptIn() {
        if (isFinishing() || notificationOptInShowing
                || !NotificationPermissionManager.shouldShowOptIn(this)) return;
        notificationOptInShowing = true;
        final boolean[] responseRecorded = {false};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.notification_opt_in_title)
                .setMessage(R.string.notification_opt_in_message)
                .setPositiveButton(R.string.notification_opt_in_accept, (ignored, which) -> {
                    responseRecorded[0] = true;
                    NotificationPermissionManager.markPromptShown(this);
                    Analytics.log(this, Analytics.NOTIFICATION_PERMISSION_PROMPT,
                            "result", "accepted_soft_prompt");
                    if (NotificationPermissionManager.hasSystemPermission(this)) {
                        NotificationPermissionManager.setOptedIn(this, true);
                        TopicManager.updateTopics(this);
                        Analytics.log(this, Analytics.NOTIFICATION_PERMISSION_RESULT,
                                "result", "granted");
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                })
                .setNegativeButton(R.string.notification_opt_in_later, (ignored, which) -> {
                    responseRecorded[0] = true;
                    NotificationPermissionManager.markPromptShown(this);
                    NotificationPermissionManager.setOptedIn(this, false);
                    TopicManager.updateTopics(this);
                    Analytics.log(this, Analytics.NOTIFICATION_PERMISSION_RESULT,
                            "result", "later");
                })
                .create();
        dialog.setOnCancelListener(ignored -> {
            if (!responseRecorded[0]) {
                responseRecorded[0] = true;
                NotificationPermissionManager.markPromptShown(this);
                NotificationPermissionManager.setOptedIn(this, false);
                TopicManager.updateTopics(this);
                Analytics.log(this, Analytics.NOTIFICATION_PERMISSION_RESULT,
                        "result", "later");
            }
        });
        dialog.setOnDismissListener(ignored -> notificationOptInShowing = false);
        dialog.show();
    }

    private void playIntroAnimationsOnce() {
        if (introAnimationsStarted) return;
        introAnimationsStarted = true;

        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
        startButton.startAnimation(pulseAnimation);
        heroSection.setAlpha(0f);
        heroSection.animate().alpha(1f).setDuration(600).setStartDelay(100).start();
        animateSparkle(sparkleViews[0], 3000, 8f);
        animateSparkle(sparkleViews[1], 2500, -6f);
        animateSparkle(sparkleViews[2], 3500, 7f);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent == null) return;

        String destination = intent.getStringExtra(EXTRA_DESTINATION);
        String link = intent.getStringExtra("link");
        String drawingId = intent.getStringExtra(DrawingExperience.EXTRA_PACK);
        intent.removeExtra(DrawingExperience.EXTRA_PACK);
        intent.removeExtra(EXTRA_DESTINATION);
        intent.removeExtra("link");

        if ((DESTINATION_DRAWING_PACK.equals(destination) || "weekly_pack".equals(destination))
                && DrawingCatalog.index(drawingId) >= 0) {
            startActivity(DrawingPackActivity.intent(this, drawingId));
            return;
        }
        if ("daily_practice".equals(destination) || isInternalLink(link, "daily_practice")) {
            // Retired notification/deep links return to the normal home, with no prompt.
            return;
        }

        String communityDocumentId = CommunityDeepLink.parseDocumentId(link);
        if (communityDocumentId != null) {
            Analytics.log(this, Analytics.NOTIFICATION_OPEN, "destination", "community_drawing");
            startActivity(ShareActivity.getDocumentStartIntent(this, communityDocumentId));
            return;
        }

        if (DESTINATION_COMMUNITY.equals(destination) || isInternalLink(link, "community", "share")) {
            Analytics.log(this, Analytics.NOTIFICATION_OPEN, "destination", DESTINATION_COMMUNITY);
            onShareClick();
            return;
        }
        if (DESTINATION_SHAPE_LIST.equals(destination) || isInternalLink(link, "shapes", "shape_list")) {
            Analytics.log(this, Analytics.NOTIFICATION_OPEN, "destination", DESTINATION_SHAPE_LIST);
            onChannelClick();
            return;
        }

        if (link != null) {
            try {
                Uri uri = Uri.parse(link);
                if ("https".equalsIgnoreCase(uri.getScheme())) {
                    Analytics.log(this, Analytics.NOTIFICATION_OPEN, "destination", "https");
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            } catch (Exception error) {
                Timber.w(error, "Invalid notification link");
            }
        }
    }

    private boolean isInternalLink(String link, String... destinations) {
        if (link == null || link.isEmpty()) return false;
        for (String destination : destinations) {
            if (destination.equalsIgnoreCase(link)) return true;
        }

        try {
            Uri uri = Uri.parse(link);
            String host = uri.getHost();
            String path = uri.getLastPathSegment();
            for (String destination : destinations) {
                if (destination.equalsIgnoreCase(host) || destination.equalsIgnoreCase(path)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // malformed links are ignored
        }
        return false;
    }

    public void onShareClick() {
        Analytics.log(this, Analytics.COMMUNITY_OPEN);
        Intent intent = ShareActivity.getRecentStartIntent(MenuActivity.this);
        startActivity(intent);
    }

    private void setupDynamicMenu() {
        boolean devMode = BuildConfig.DEBUG;
        MenuConfig cfg = MenuConfigLoader.loadFromRemoteConfig(this, devMode, remoteConfig -> {
            if (isFinishing() || isDestroyed()) return;
            if (remoteConfig != null && remoteConfig.items != null && !remoteConfig.items.isEmpty()) {
                renderDynamicMenu(remoteConfig);
            }
        });
        if (cfg == null) {
            cfg = MenuConfigLoader.loadFromAssets(this);
        }
        if (cfg == null || cfg.items == null || cfg.items.isEmpty()) {
            // keep fallback visible
            return;
        }

        renderDynamicMenu(cfg);
    }

    private void renderDynamicMenu(MenuConfig cfg) {
        activeMenuConfigVersion = cfg.version;
        activeMenuExperiment = cfg.experiment == null ? "" : cfg.experiment;
        // Configure Recycler
        if (dynamicAdapter == null) {
            GridLayoutManager glm = new GridLayoutManager(this, 2);
            rvDynamicMenu.setLayoutManager(glm);
            dynamicAdapter = new DynamicMenuAdapter(this, this::handleDynamicAction);
            rvDynamicMenu.setAdapter(dynamicAdapter);
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override public int getSpanSize(int position) {
                    if (dynamicAdapter == null) return 1;
                    int vt = dynamicAdapter.getItemViewType(position);
                    // promo 타입은 두 칸을 차지
                    return vt == 1 ? 2 : 1;
                }
            });
        }
        dynamicAdapter.submit(cfg.items);

        // Hide fallback static views (grid/card) but keep hero start button
        View grid = findViewById(R.id.grid_actions);
        View card = findViewById(R.id.card_actions);
        if (grid != null) grid.setVisibility(View.GONE);
        if (card != null) card.setVisibility(View.GONE);

        rvDynamicMenu.setVisibility(View.VISIBLE);
        rvDynamicMenu.post(this::trackVisiblePromoImpressions);
    }

    private void trackVisiblePromoImpressions() {
        if (!homeResumed || !hasWindowFocus() || dynamicAdapter == null || !rvDynamicMenu.isShown()) {
            return;
        }
        for (int index = 0; index < rvDynamicMenu.getChildCount(); index++) {
            View child = rvDynamicMenu.getChildAt(index);
            RecyclerView.ViewHolder holder = rvDynamicMenu.getChildViewHolder(child);
            int position = holder.getBindingAdapterPosition();
            MenuItemConfig item = dynamicAdapter.getItem(position);
            if (item == null || !(item.promo || item.span >= 2)) {
                continue;
            }

            Rect visibleRect = new Rect();
            if (!child.getGlobalVisibleRect(visibleRect)) {
                continue;
            }
            long totalArea = (long) child.getWidth() * child.getHeight();
            long visibleArea = (long) visibleRect.width() * visibleRect.height();
            String itemId = item.id == null || item.id.isEmpty()
                    ? "position_" + position
                    : item.id;
            String impressionKey = activeMenuConfigVersion + "|" + activeMenuExperiment + "|" + itemId;
            if (promoImpressions.recordIfVisible(impressionKey, homeResumed, hasWindowFocus(),
                    visibleArea, totalArea)) {
                logCrossPromoEvent(Analytics.CROSS_PROMO_IMPRESSION, item);
            }
        }
    }

    private void logCrossPromoEvent(String event, MenuItemConfig item) {
        logCrossPromoEvent(event, item, null);
    }

    private void logCrossPromoEvent(String event, MenuItemConfig item, String openResult) {
        Bundle params = new Bundle();
        params.putString("placement", "home_menu");
        params.putLong("config_version", activeMenuConfigVersion);
        params.putString("experiment", activeMenuExperiment);
        params.putString("source_package", BuildConfig.APPLICATION_ID);
        if (openResult != null) params.putString("open_result", openResult);
        if (item.id != null) {
            params.putString("item_id", item.id);
        }
        if (item.payload != null) {
            try {
                String targetPackage = Uri.parse(item.payload).getQueryParameter("id");
                if (targetPackage != null) {
                    params.putString("target_package", targetPackage);
                }
            } catch (Exception ignored) {
                // 잘못된 원격 URL이어도 메뉴 렌더링/클릭을 막지 않는다.
            }
        }
        Analytics.log(this, event, params);
        if (BuildConfig.DEBUG) Timber.d("CrossPromo %s %s", event, params);
    }

    private void logPromoOpenResult(MenuItemConfig item, String result) {
        if (item.promo || item.span >= 2) {
            logCrossPromoEvent(Analytics.CROSS_PROMO_OPEN, item, result);
        }
    }

    private void openConfiguredUrl(MenuItemConfig item) {
        if (item.payload == null || item.payload.isEmpty()) {
            logPromoOpenResult(item, "missing_url");
            return;
        }
        Uri webUri;
        try {
            webUri = Uri.parse(item.payload);
        } catch (Exception error) {
            logPromoOpenResult(item, "invalid_url");
            Timber.w(error, "Invalid configured url");
            return;
        }

        boolean promo = item.promo || item.span >= 2;
        if (promo) {
            if (!webUri.isHierarchical()) {
                logPromoOpenResult(item, "invalid_url");
                Timber.w("Blocked non-hierarchical cross-promo url: %s", item.payload);
                return;
            }
            String targetPackage = webUri.getQueryParameter("id");
            boolean allowedPackage = "com.h2play.magicnf".equals(targetPackage)
                    || "com.h2play.remoteMagic".equals(targetPackage)
                    || "com.todayscrossword.app".equals(targetPackage)
                    || "com.h2play.chainlab".equals(targetPackage);
            boolean allowedUrl = "https".equalsIgnoreCase(webUri.getScheme())
                    && "play.google.com".equalsIgnoreCase(webUri.getHost())
                    && "/store/apps/details".equals(webUri.getPath());
            if (!allowedPackage || !allowedUrl) {
                logPromoOpenResult(item, "blocked_destination");
                Timber.w("Blocked invalid cross-promo target: %s", item.payload);
                return;
            }

            Uri.Builder marketUri = new Uri.Builder()
                    .scheme("market")
                    .authority("details")
                    .appendQueryParameter("id", targetPackage);
            String referrer = webUri.getQueryParameter("referrer");
            if (referrer != null && !referrer.isEmpty()) {
                marketUri.appendQueryParameter("referrer", referrer);
            }
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, marketUri.build());
            playStoreIntent.setPackage("com.android.vending");
            try {
                startActivity(playStoreIntent);
                logPromoOpenResult(item, "market_intent_started");
                return;
            } catch (android.content.ActivityNotFoundException error) {
                Timber.i(error, "Google Play app unavailable; falling back to HTTPS");
            }
        } else if (!"https".equalsIgnoreCase(webUri.getScheme())) {
            Timber.w("Blocked non-HTTPS configured url: %s", item.payload);
            return;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            logPromoOpenResult(item, "web_intent_started");
        } catch (android.content.ActivityNotFoundException error) {
            logPromoOpenResult(item, "no_handler");
            Timber.w(error, "No activity to handle url: %s", item.payload);
        }
    }

    private void handleDynamicAction(MenuItemConfig item) {
        if (item == null) return;
        String action = item.action == null ? "" : item.action;

        Bundle mb = new Bundle();
        mb.putString("action", action);
        if (item.id != null) mb.putString("item_id", item.id);
        Analytics.log(this, Analytics.MENU_ACTION, mb);
        if (item.promo || item.span >= 2) {
            logCrossPromoEvent(Analytics.CROSS_PROMO_CLICK, item);
        }

        switch (action) {
            case "open_url":
                openConfiguredUrl(item);
                break;
            case "rate":
                onRateClick();
                break;
            case "share_link":
                onShareLinkClick();
                break;
            case "more_apps":
                onMoreClick();
                break;
            case "start_tutorial":
                onStartClick();
                break;
            case "shape_list":
                onChannelClick();
                break;
            case "open_help":
                onHelpClick();
                break;
            case "open_share":
                onShareClick();
                break;
            case "restart_tutorial":
                onRestartTutorialClick();
                break;
            default:
                // no-op
        }
    }

    /**
     * 튜토리얼 다시하기 버튼 클릭 핸들러
     * 확인 대화상자를 표시하고, 사용자가 확인하면 가이드를 다시 활성화한 후 튜토리얼 시작
     */
    public void onRestartTutorialClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name)
                .setMessage(R.string.restart_tutorial_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // 가이드 다시 활성화
                    menuPresenter.restartTutorial();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private static final int REQUEST_TUTORIAL_DIALOGUE = 2001;

    @Override
    public void startTutorial() {
        Analytics.log(this, Analytics.TUTORIAL_START);
        TutorialProgress progress = menuPresenter.getTutorialProgress();
        if (progress != TutorialProgress.NOT_STARTED) {
            startActivity(MainActivity.getStartIntent(this, 0)
                    .putExtra(MainActivity.EXTRA_ENTRY_POINT, "tutorial_resume"));
            return;
        }
        Intent intent = com.h2play.canvas_magic.features.tutorial.TutorialDialogueActivity
                .getStartIntent(this, com.h2play.canvas_magic.features.tutorial.TutorialDialogueActivity.PHASE_PRE_DRAW);
        startActivityForResult(intent, REQUEST_TUTORIAL_DIALOGUE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TUTORIAL_DIALOGUE) {
            if (resultCode == RESULT_OK) {
                menuPresenter.setTutorialProgress(TutorialProgress.DRAWING_GUIDE);
                Intent intent = MainActivity.getStartIntent(this, 0);
                startActivity(intent);
            } else {
                // Keep NOT_STARTED so the next tap resumes the lesson.
                menuPresenter.cancelTutorial();
            }
        }
    }

    @Override
    public void showShapeList(List<ShapeInfo> shapeInfos) {
        PackPickerBottomSheet.show(this, shapeInfos, menuPresenter.getLastPackRef(),
                pack -> startPack(pack, "pack_picker"));
    }

    @Override
    public void showProgress(boolean show) {
        // Implementation
    }

    @Override
    public void showError(Throwable error) {
        errorView.setVisibility(View.VISIBLE);
        Timber.e(error, "There was an error retrieving the data");
    }

    @Override
    public void showTutorial() {
        tvStartLabel.setText(menuPresenter.getTutorialProgress() == TutorialProgress.NOT_STARTED
                ? R.string.start_tutorial : R.string.resume_practice);
        findViewById(R.id.tv_active_pack).setVisibility(View.GONE);
    }

    @Override
    public void onReloadData() {
        // Implementation
    }

    private void animateSparkle(View v, long duration, float drift) {
        if (v == null || v.getVisibility() != View.VISIBLE) return;
        v.animate()
                .translationYBy(drift)
                .alpha(0.3f)
                .setDuration(duration)
                .withEndAction(() -> v.animate()
                        .translationYBy(-drift)
                        .alpha(1f)
                        .setDuration(duration)
                        .withEndAction(() -> {
                            if (v.getVisibility() == View.VISIBLE) {
                                animateSparkle(v, duration, drift);
                            }
                        })
                        .start())
                .start();
    }
}

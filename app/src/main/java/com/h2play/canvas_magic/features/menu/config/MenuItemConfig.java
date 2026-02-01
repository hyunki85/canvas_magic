package com.h2play.canvas_magic.features.menu.config;

import androidx.annotation.Nullable;

/**
 * 메뉴 아이템 설정 모델
 */
public class MenuItemConfig {
    public String id;            // 고유 ID (ab 테스트 분기 등에 사용)
    public String title;         // 버튼 라벨
    @Nullable
    public String icon;          // drawable 리소스 이름 (예: "ic_help")
    @Nullable
    public String imageUrl;      // 원격 이미지 URL (https:// 또는 gs://)
    @Nullable
    public String bgColor;       // 카드 배경색 (예: "#3F51B5"), 없으면 팔레트 순환 적용
    @Nullable
    public String description;   // 카드 부제목 (짧은 설명)
    public String action;        // 동작 타입 (open_url, rate, share_link, more_apps, start_tutorial, shape_list, open_help, open_share, restart_tutorial)
    @Nullable
    public String payload;       // action 에 필요한 데이터 (예: url)Y

    /**
     * 그리드 스팬 크기 (기본 1). 2로 설정하면 2칸을 차지하는 프로모션 카드로 표시됩니다.
     * Gson 기본값(0)인 경우 어댑터에서 1로 보정합니다.
     */
    public int span;             // 1 또는 2
    /**
     * 프로모션 스타일 여부(선택). true 이거나 span>=2 인 경우 큰 이미지 레이아웃을 사용합니다.
     */
    public boolean promo;
    /**
     * 소형 카드 표시 여부. true면 낮은 높이의 가로형 카드로 렌더링됩니다.
     */
    public boolean small;

    // 빈 생성자 (Gson)
    public MenuItemConfig() {}
}

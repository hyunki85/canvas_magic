package com.h2play.canvas_magic.features.menu.config;

import java.util.List;

/**
 * 메뉴 설정 루트 모델
 */
public class MenuConfig {
    public int version;
    public String experiment; // A/B 테스트 버전 라벨 등
    public List<MenuItemConfig> items;

    public MenuConfig() {}
}

package com.enigma;

import java.util.List;

/**
 * 单次按键结果:灯板亮起的字母、按键后三转子位置、完整信号路径
 *
 * @author echo
 * @date 2026-10-19
 */
/**
 * 单次按键结果:灯板亮起的字母、按键后三转子位置、完整信号路径
 *
 * @param lamp           灯板亮起的密文字母
 * @param rotorPositions 按键后三转子位置(如 "ABL")
 * @param path           信号路径 12 站追踪
 */
public record KeyPressResult(char lamp, String rotorPositions, List<TracePoint> path) {

    @Override
    public String toString() {
        return "lamp=" + lamp + ", positions=" + rotorPositions + ", path=" + path;
    }
}

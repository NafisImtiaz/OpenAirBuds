package com.openairbuds.app.protocol

import java.util.UUID

object OpoConstants {
    /**
     * Standard SDP service UUIDs for OPPO/realme control service.
     */
    val UUID_OPPO_INTERACTION: UUID = UUID.fromString("0000079A-D102-11E1-9B23-00025B00A5A5")
    val UUID_OPPO_SECONDARY: UUID = UUID.fromString("00001107-D102-11E1-9B23-00025B00A5A5")

    /**
     * Sync marker magic byte.
     */
    const val SYNC_BYTE: Byte = 0xAA.toByte()

    /**
     * 16-bit Little-Endian Command IDs.
     */
    const val CMD_HANDSHAKE: Short = 0x0100.toShort()
    const val CMD_QUERY_PRODUCT_ID: Short = 0x0103.toShort()
    const val CMD_QUERY_FIRMWARE: Short = 0x0105.toShort()
    const val CMD_QUERY_BATTERY: Short = 0x0106.toShort()
    const val CMD_QUERY_KEY_FUNCTION: Short = 0x0108.toShort()
    const val CMD_QUERY_WEARING: Short = 0x0109.toShort()
    const val CMD_QUERY_ANC: Short = 0x010C.toShort()
    const val CMD_QUERY_FEATURES: Short = 0x010D.toShort()
    const val CMD_QUERY_EQ: Short = 0x010F.toShort()
    const val CMD_QUERY_DEVICES: Short = 0x0112.toShort()
    const val CMD_QUERY_BASSWAVE: Short = 0x0124.toShort()
    const val CMD_QUERY_BROADCAST: Short = 0x0200.toShort()
    const val CMD_REGISTER_ONE: Short = 0x0201.toShort()
    const val CMD_ACTIVE_REPORT: Short = 0x0204.toShort()
    const val CMD_REGISTER_NOTIFY: Short = 0x0205.toShort()

    const val CMD_FIND_BUDS: Short = 0x0400.toShort()
    const val CMD_SET_KEY_FUNCTION: Short = 0x0401.toShort()
    const val CMD_SET_FEATURE: Short = 0x0403.toShort()
    const val CMD_SET_ANC: Short = 0x0404.toShort()
    const val CMD_SUB_HOLD_CYCLE: Byte = 0x02
    const val HOLD_CYCLE_OFF: Byte = 0x01
    const val HOLD_CYCLE_TRANSPARENCY: Byte = 0x02
    const val HOLD_CYCLE_ANC: Byte = 0x08

    const val CMD_FIT_TEST: Short = 0x0405.toShort()
    const val CMD_SET_EQ: Short = 0x0406.toShort()
    const val CMD_SET_CUSTOM_EQ: Short = 0x0418.toShort()
    const val CMD_SET_CUSTOM_EQ_REALME: Short = 0x0418.toShort()
    const val CMD_SET_BASSWAVE_LEVEL: Short = 0x041B.toShort()

    const val CMD_CLOCK_SYNC_REQ: Short = 0x0500.toShort()
    const val CMD_FEATURE_PUSH: Short = 0x0503.toShort()
    const val CMD_EQ_CHANGED: Short = 0x0504.toShort()

    // Response Command IDs (Request with bit 15 set)
    const val CMD_RESP_HANDSHAKE: Short = 0x8100.toShort()
    const val CMD_RESP_PRODUCT_ID: Short = 0x8103.toShort()
    const val CMD_RESP_FIRMWARE: Short = 0x8105.toShort()
    const val CMD_RESP_BATTERY: Short = 0x8106.toShort()
    const val CMD_RESP_KEY_FUNCTION: Short = 0x8108.toShort()
    const val CMD_RESP_WEARING: Short = 0x8109.toShort()
    const val CMD_RESP_ANC: Short = 0x810C.toShort()
    const val CMD_RESP_FEATURES: Short = 0x810D.toShort()
    const val CMD_RESP_EQ: Short = 0x810F.toShort()
    const val CMD_RESP_DEVICES: Short = 0x8112.toShort()
    const val CMD_RESP_BASSWAVE: Short = 0x8124.toShort()
    const val CMD_RESP_BROADCAST: Short = 0x8200.toShort()
    const val CMD_RESP_REGISTER_NOTIFY: Short = 0x8205.toShort()
    const val CMD_CLOCK_SYNC_RESP: Short = 0x8500.toShort()

    /**
     * Subtypes inside 0x0204 active reports.
     */
    const val EVT_BATTERY: Byte = 0x01
    const val EVT_WEAR: Byte = 0x02
    const val EVT_ANC: Byte = 0x03
    const val EVT_FIT_TEST: Byte = 0x04
    const val EVT_GAME_MODE: Byte = 0x05
    const val EVT_DUAL_DEVICES: Byte = 0x06

    /**
     * Feature switch IDs (used in 0x0403, 0x010D, 0x0503).
     */
    const val FEATURE_IN_EAR_DETECTION: Byte = 0x04
    const val FEATURE_GAME_MODE: Byte = 0x06
    const val FEATURE_ENHANCE_VOICE: Byte = 0x09
    const val FEATURE_WIND_NOISE_REDUCTION: Byte = 0x0C
    const val FEATURE_DUAL_CONNECTION: Byte = 0x11
    const val FEATURE_HI_RES_AUDIO: Byte = 0x18
    const val FEATURE_BASSWAVE: Byte = 0x1D
    const val FEATURE_VOLUME_ENHANCER: Byte = 0x1E

    const val GESTURE_FN_SWITCH_DEVICES: Byte = 0x09

    /**
     * Legacy opcodes kept for backwards compatibility.
     */
    const val OPCODE_STATUS_REPORT: Short = CMD_ACTIVE_REPORT
    const val PAYLOAD_TYPE_BATTERY: Byte = EVT_BATTERY
    const val PAYLOAD_TYPE_PLACEMENT: Byte = EVT_WEAR
    const val PAYLOAD_TYPE_NOISE_MODE: Byte = EVT_ANC

    /**
     * Slots for battery & placement.
     */
    const val SLOT_LEFT: Byte = 0x01
    const val SLOT_RIGHT: Byte = 0x02
    const val SLOT_CASE: Byte = 0x03

    /**
     * Placement values.
     */
    const val PLACEMENT_IN_CASE: Byte = 0x00
    const val PLACEMENT_IN_EAR: Byte = 0x01
    const val PLACEMENT_IN_USE: Byte = 0x03
    const val PLACEMENT_OUT_OF_CASE: Byte = 0x04

    /**
     * Wire mode values for Realme Buds:
     * Mode and ANC level ride in the exact same byte.
     */
    const val WIRE_NOISE_OFF: Byte = 0x01
    const val WIRE_NOISE_TRANSPARENCY: Byte = 0x02
    const val WIRE_ANC_MILD: Byte = 0x04
    const val WIRE_ANC_MAX: Byte = 0x08        // Deep 50dB on Realme Buds Air 6
    const val WIRE_ANC_MODERATE: Byte = 0x10
    const val WIRE_ANC_SMART: Byte = 0x20
}

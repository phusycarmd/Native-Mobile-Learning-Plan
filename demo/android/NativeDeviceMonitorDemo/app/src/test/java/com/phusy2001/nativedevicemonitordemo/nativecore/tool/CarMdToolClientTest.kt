package com.phusy2001.nativedevicemonitordemo.nativecore.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Mẫu dữ liệu lấy từ Bluetooth_CommandSet spec và carmd-connect-app (fakers)
class CarMdToolClientTest {

    private fun hex(s: String): ByteArray =
        s.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it) }

    // Body = frame bỏ 4 byte header (DA, id, lenLo, lenHi) và 1 byte checksum
    private fun body(frame: String): ByteArray = hex(frame).let { it.copyOfRange(4, it.size - 1) }

    @Test
    fun buildRequest_matchesSpecBytes() {
        assertEquals("AD E1 01 00 8F", CarMdToolClient.buildRequest(0xE1, byteArrayOf(0x00)).toHex())
        assertEquals("AD E5 00 92", CarMdToolClient.buildRequest(0xE5).toHex())
        assertEquals("AD E7 00 94", CarMdToolClient.buildRequest(0xE7).toHex())
        assertEquals("AD F3 01 02 A3", CarMdToolClient.buildRequest(0xF3, byteArrayOf(0x02)).toHex())
    }

    @Test
    fun parseToolSetting_fakerResponse() {
        val frame = "dae220005630312e30312e3035000000000000005630312e30342e3033000000000000008f"
        val (bootloader, firmware) = CarMdToolClient.parseToolSetting(body(frame))!!
        assertEquals("V01.01.05", bootloader)
        assertEquals("V01.04.03", firmware)
    }

    @Test
    fun parseGuid_swapsDotNetByteOrder() {
        val raw = hex("44cf4f308522c14bb18f7c51dae1428e")
        assertEquals("304fcf44-2285-4bc1-b18f-7c51dae1428e", CarMdToolClient.parseGuid(raw))
        assertNull(CarMdToolClient.parseGuid(ByteArray(16)))
    }

    @Test
    fun parseUsbProductId_littleEndianUnsigned() {
        assertEquals(0x0318, CarMdToolClient.parseUsbProductId(body("dae802001803df")))
        assertEquals(0xDAAD, CarMdToolClient.parseUsbProductId(body("DAE80200ADDA4B")))
    }
}

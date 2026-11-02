package com.startrack.remote

import android.app.Activity
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface

class MainActivity : Activity() {
    private lateinit var ir: ConsumerIrManager
    private lateinit var status: TextView

    // NEC-style starter codes (address 0x00, command values).
    // These are generic test codes, NOT confirmed Star Track codes.
    private val commands = mapOf(
        "Power" to 0x0C, "Mute" to 0x0D, "Vol +" to 0x10, "Vol −" to 0x11,
        "CH +" to 0x20, "CH −" to 0x21, "Up" to 0x18, "Down" to 0x19,
        "Left" to 0x1A, "Right" to 0x1B, "OK" to 0x1C,
        "Menu" to 0x0E, "Back" to 0x0F, "Exit" to 0x1D,
        "Source" to 0x1E, "0" to 0x00, "1" to 0x01, "2" to 0x02,
        "3" to 0x03, "4" to 0x04, "5" to 0x05, "6" to 0x06,
        "7" to 0x07, "8" to 0x08, "9" to 0x09,
        "Home" to 0x22, "Netflix" to 0x23, "YouTube" to 0x24,
        "Freeze" to 0x25, "P.Mode" to 0x26, "S.Mode" to 0x27
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ir = getSystemService(CONSUMER_IR_SERVICE) as ConsumerIrManager
        buildUi()
        updateStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(16, 24, 32))
        }
        val title = TextView(this).apply {
            text = "STAR TRACK\nREMOTE PRO"
            textSize = 25f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(230, 194, 115)); typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 12, 0, 18)
        }
        root.addView(title)
        status = TextView(this).apply {
            textSize = 13f; gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY); setPadding(0, 0, 0, 16)
        }
        root.addView(status)
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        val power = Button(this).apply { text = "⏻   POWER"; textSize = 18f }
        power.setOnClickListener { send("Power") }
        body.addView(power, matchWrap())
        addRow(body, listOf("Vol −", "Mute", "Vol +"))
        addRow(body, listOf("1", "2", "3"))
        addRow(body, listOf("4", "5", "6"))
        addRow(body, listOf("7", "8", "9"))
        addRow(body, listOf("P.Mode", "0", "S.Mode"))
        addRow(body, listOf("Home", "Netflix", "YouTube"))
        addRow(body, listOf("CH −", "Menu", "CH +"))
        addRow(body, listOf(" ", "Up", " "))
        addRow(body, listOf("Left", "OK", "Right"))
        addRow(body, listOf(" ", "Down", " "))
        addRow(body, listOf("Back", "Source", "Exit"))
        addRow(body, listOf("Freeze", "", ""))
        val note = TextView(this).apply {
            text = "\nالجهاز المستهدف: STAR TRACK ST-43D-AZ2000 • الريموت: AD1783. الأكواد المدمجة تجريبية بصيغة NEC وليست مستخرجة من الريموت الأصلي أو مؤكدة لهذا الموديل؛ قد لا تستجيب الأزرار حتى تتم مطابقة بروتوكول IR الصحيح.\n\nوجّه أعلى الهاتف نحو التلفزيون ومن مسافة 1–3 أمتار."
            setTextColor(Color.LTGRAY); textSize = 13f; gravity = Gravity.CENTER
        }
        body.addView(note)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 }
    private fun addRow(parent: LinearLayout, labels: List<String>) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        labels.forEach { label ->
            val b = Button(this).apply {
                text = label; isAllCaps = false
                textSize = 14f
                isEnabled = label.isNotBlank()
                setOnClickListener { send(label) }
            }
            row.addView(b, LinearLayout.LayoutParams(0, 58, 1f).apply {
                setMargins(3, 3, 3, 3)
            })
        }
        parent.addView(row)
    }

    private fun updateStatus() {
        status.text = when {
            !ir.hasIrEmitter() -> "لم يتم اكتشاف مرسل IR في هذا الهاتف"
            else -> "تم اكتشاف مرسل IR • جاهز للإرسال"
        }
    }

    private fun send(name: String) {
        if (!ir.hasIrEmitter()) {
            Toast.makeText(this, "هذا الهاتف لا يعلن عن مرسل IR متاح", Toast.LENGTH_LONG).show()
            return
        }
        val freq = 38000
        val command = commands[name]
        if (command == null) return
        val pattern = nec(0x00, command)
        try {
            ir.transmit(freq, pattern)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر إرسال الأشعة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun nec(address: Int, command: Int): IntArray {
        val bits = (address and 0xFF) or ((address.inv() and 0xFF) shl 8) or
                ((command and 0xFF) shl 16) or ((command.inv() and 0xFF) shl 24)
        val out = ArrayList<Int>()
        out.add(9000); out.add(4500)
        for (i in 0 until 32) {
            out.add(560)
            out.add(if (((bits ushr i) and 1) == 1) 1690 else 560)
        }
        out.add(560)
        return out.toIntArray()
    }
}

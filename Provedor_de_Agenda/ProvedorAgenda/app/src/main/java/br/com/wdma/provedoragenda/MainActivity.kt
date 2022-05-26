package br.com.wdma.provedoragenda

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Events.*
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Creating button
        val btnSetEvent = findViewById<Button>(R.id.set_event)
        btnSetEvent.setOnClickListener{

            // Creating a intent
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CONTENT_URI)
                .putExtra(TITLE, "Bootcamp Everis")
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis()+(60*60*1000))
                .putExtra(EVENT_LOCATION, "On Line")
                .putExtra(CALENDAR_DISPLAY_NAME, "Selecione o horário")
                .putExtra(CalendarContract.ACTION_EVENT_REMINDER, "Lembre-me")

            // Start intent
            startActivity(intent)

        }
    }
}
package zm.ac.mulungushi.ict361;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ScheduleActivity extends AppCompatActivity {

    private final String[] schedule = {
            "08:00 - 10:00 AM | ICT 361: Mobile App Dev (Lab 2)",
            "11:00 - 01:00 PM | ICT 311: OOP II Lecture (LT 1)",
            "02:00 - 04:00 PM | ICT 412: Database Systems (Lab 1)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ListView listView = findViewById(R.id.listViewSchedule);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list_row, schedule);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = schedule[position];
            Toast.makeText(this, "Class Details: " + selected, Toast.LENGTH_SHORT).show();
        });
    }
}

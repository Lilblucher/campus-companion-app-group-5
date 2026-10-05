package zm.ac.mulungushi.ict361;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CoursesActivity extends AppCompatActivity {

    private final String[] courses = {
            "ICT 361 - Mobile Application Development",
            "ICT 311 - Object Oriented Programming II",
            "ICT 412 - Database Management Systems",
            "ICT 221 - Data Structures & Algorithms",
            "ICT 451 - Software Engineering & Architecture"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ListView listView = findViewById(R.id.listViewCourses);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list_row, courses);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = courses[position];
            Toast.makeText(this, "Course Selected: " + selected, Toast.LENGTH_SHORT).show();
        });
    }
}

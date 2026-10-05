package zm.ac.mulungushi.ict361;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentsActivity extends AppCompatActivity {

    private final String[] students = {
            "20230001 - John Banda (BSc Computer Science)",
            "20230002 - Mary Phiri (BSc Information Technology)",
            "20230003 - Mwape Lungu (BSc Software Engineering)",
            "20230004 - Chisomo Tembo (BSc Computer Science)",
            "20230005 - Kelvin Mulenga (BSc Information Systems)",
            "20230006 - Bwalya Chanda (BSc Computer Science)",
            "20230007 - Natasha Zimba (BSc Software Engineering)",
            "20230008 - Mutale Kasonde (BSc Information Technology)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_students);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ListView listView = findViewById(R.id.listViewStudents);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list_row, students);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = students[position];
            Toast.makeText(this, "Student Selected: " + selected, Toast.LENGTH_SHORT).show();
        });
    }
}

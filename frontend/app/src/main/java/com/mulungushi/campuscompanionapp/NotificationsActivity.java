package zm.ac.mulungushi.ict361;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NotificationsActivity extends AppCompatActivity {

    private final String[] notifications = {
            "🔔 New Registration Request from John Banda (ICT361)",
            "🔔 Faculty Meeting scheduled for 14:00 PM Tomorrow",
            "🔔 ICT311 Course Syllabus Approval Confirmed",
            "🔔 System Maintenance Notice: Saturday 20:00 - 22:00"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ListView listView = findViewById(R.id.listViewNotifications);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list_row, notifications);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = notifications[position];
            Toast.makeText(this, "Notification: " + selected, Toast.LENGTH_SHORT).show();
        });
    }
}

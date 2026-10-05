package zm.ac.mulungushi.ict361;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PendingRequestsActivity extends AppCompatActivity {

    private final String[] requests = {
            "Request #101: John Banda - Registration Approval (ICT 361)",
            "Request #102: Mary Phiri - Exemption Request (ICT 311)",
            "Request #103: Mwape Lungu - Group Transfer (ICT 412)",
            "Request #104: Chisomo Tembo - Late Registration Approval"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ListView listView = findViewById(R.id.listViewRequests);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list_row, requests);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = requests[position];
            Toast.makeText(this, "Approved: " + selected, Toast.LENGTH_SHORT).show();
        });
    }
}

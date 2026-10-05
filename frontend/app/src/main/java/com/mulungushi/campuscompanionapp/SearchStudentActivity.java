package zm.ac.mulungushi.ict361;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SearchStudentActivity extends AppCompatActivity {

    private final String[] allStudents = {
            "20230001 - John Banda (BSc Computer Science)",
            "20230002 - Mary Phiri (BSc Information Technology)",
            "20230003 - Mwape Lungu (BSc Software Engineering)",
            "20230004 - Chisomo Tembo (BSc Computer Science)",
            "20230005 - Kelvin Mulenga (BSc Information Systems)",
            "20230006 - Bwalya Chanda (BSc Computer Science)",
            "20230007 - Natasha Zimba (BSc Software Engineering)",
            "20230008 - Mutale Kasonde (BSc Information Technology)"
    };

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_student);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        EditText etSearchQuery = findViewById(R.id.etSearchQuery);
        ListView listView = findViewById(R.id.listViewSearchResults);

        adapter = new ArrayAdapter<>(this, R.layout.item_list_row, allStudents);
        listView.setAdapter(adapter);

        etSearchQuery.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = adapter.getItem(position);
            Toast.makeText(this, "Student Found: " + selected, Toast.LENGTH_SHORT).show();
        });
    }
}

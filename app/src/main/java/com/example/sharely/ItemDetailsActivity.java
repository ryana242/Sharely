package com.example.sharely;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

import android.widget.Toast;

import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.example.sharely.service.BookingService;

import java.util.ArrayList;
import java.util.List;

public class ItemDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.itemDetailsMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView itemName = findViewById(R.id.textItemName);
        TextView itemPrice = findViewById(R.id.textItemPrice);
        TextView itemInfo = findViewById(R.id.textItemInfo);
        TextView itemDescription = findViewById(R.id.textItemDescription);

        Intent intent = getIntent();

        itemName.setText(intent.getStringExtra("name"));
        itemPrice.setText(intent.getStringExtra("price"));
        itemInfo.setText(intent.getStringExtra("info"));
        itemDescription.setText(intent.getStringExtra("description"));
        Button backButton = findViewById(R.id.buttonBack);

        backButton.setOnClickListener(v -> {
            finish();
        });

        Button requestBookingButton =
                findViewById(R.id.buttonRequestBooking);

        requestBookingButton.setOnClickListener(v -> {

            AvailabilitySlot requestedSlot =
                    new AvailabilitySlot(1000, 5000);

            Booking newBooking = new Booking(
                    "B002",
                    "CURRENT_USER",
                    "ITEM1",
                    requestedSlot
            );

            List<Booking> existingBookings = new ArrayList<>();

            BookingService bookingService = new BookingService();

            boolean accepted =
                    bookingService.acceptBooking(
                            newBooking,
                            existingBookings
                    );

            if (accepted) {
                Toast.makeText(
                        ItemDetailsActivity.this,
                        "Booking accepted",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        ItemDetailsActivity.this,
                        "Booking unavailable",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

//        Booking newBooking = new Booking(
//                "B002",
//                "CURRENT_USER",
//                "ITEM1",
//                requestedSlot
//        );
//
//        List<Booking> existingBookings = new ArrayList<>();
//
//        BookingService bookingService = new BookingService();
//
//        boolean accepted =
//                bookingService.acceptBooking(
//                        newBooking,
//                        existingBookings
//                );
    }
}
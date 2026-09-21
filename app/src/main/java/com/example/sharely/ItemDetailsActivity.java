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

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

public class ItemDetailsActivity extends AppCompatActivity {

    private Calendar startCalendar;
    private Calendar endCalendar;

    private boolean startSelected = false;
    private boolean endSelected = false;

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


            if (!startSelected || !endSelected) {
                Toast.makeText(
                        ItemDetailsActivity.this,
                        "Please select both start and end time",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            AvailabilitySlot requestedSlot =
                    new AvailabilitySlot(
                            startCalendar.getTimeInMillis(),
                            endCalendar.getTimeInMillis()
                    );

            Booking newBooking = new Booking(
                    "B002",
                    "CURRENT_USER",
                    "ITEM1",
                    requestedSlot
            );

            List<Booking> existingBookings = new ArrayList<>();

//            Booking existingBooking = new Booking(
//                    "B001",
//                    "ANOTHER_USER",
//                    "ITEM1",
//                    new AvailabilitySlot(3000, 7000)
//            );
//
//            existingBooking.acceptBooking();
//
//            existingBookings.add(existingBooking);

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

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();

        TextView startTimeText =
                findViewById(R.id.textStartTime);

        TextView endTimeText =
                findViewById(R.id.textEndTime);

        Button selectStartButton =
                findViewById(R.id.buttonSelectStart);

        Button selectEndButton =
                findViewById(R.id.buttonSelectEnd);

        selectStartButton.setOnClickListener(v -> {

            DatePickerDialog datePicker = new DatePickerDialog(
                    ItemDetailsActivity.this,
                    (view, year, month, dayOfMonth) -> {

                        startCalendar.set(
                                Calendar.YEAR,
                                year
                        );

                        startCalendar.set(
                                Calendar.MONTH,
                                month
                        );

                        startCalendar.set(
                                Calendar.DAY_OF_MONTH,
                                dayOfMonth
                        );

                        TimePickerDialog timePicker =
                                new TimePickerDialog(
                                        ItemDetailsActivity.this,
                                        (timeView, hourOfDay, minute) -> {

                                            startCalendar.set(
                                                    Calendar.HOUR_OF_DAY,
                                                    hourOfDay
                                            );

                                            startCalendar.set(
                                                    Calendar.MINUTE,
                                                    minute
                                            );

                                            startCalendar.set(
                                                    Calendar.SECOND,
                                                    0
                                            );

                                            startSelected = true;

                                            String text =
                                                    "Start: "
                                                            + dayOfMonth + "/"
                                                            + (month + 1) + "/"
                                                            + year + " "
                                                            + String.format(
                                                            "%02d:%02d",
                                                            hourOfDay,
                                                            minute
                                                    );

                                            startTimeText.setText(text);
                                        },
                                        startCalendar.get(Calendar.HOUR_OF_DAY),
                                        startCalendar.get(Calendar.MINUTE),
                                        true
                                );

                        timePicker.show();
                    },
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH)
            );

            datePicker.show();
        });

        selectEndButton.setOnClickListener(v -> {

            DatePickerDialog datePicker = new DatePickerDialog(
                    ItemDetailsActivity.this,
                    (view, year, month, dayOfMonth) -> {

                        endCalendar.set(
                                Calendar.YEAR,
                                year
                        );

                        endCalendar.set(
                                Calendar.MONTH,
                                month
                        );

                        endCalendar.set(
                                Calendar.DAY_OF_MONTH,
                                dayOfMonth
                        );

                        endSelected = true;

                        TimePickerDialog timePicker =
                                new TimePickerDialog(
                                        ItemDetailsActivity.this,
                                        (timeView, hourOfDay, minute) -> {

                                            endCalendar.set(
                                                    Calendar.HOUR_OF_DAY,
                                                    hourOfDay
                                            );

                                            endCalendar.set(
                                                    Calendar.MINUTE,
                                                    minute
                                            );

                                            endCalendar.set(
                                                    Calendar.SECOND,
                                                    0
                                            );

                                            String text =
                                                    "End: "
                                                            + dayOfMonth + "/"
                                                            + (month + 1) + "/"
                                                            + year + " "
                                                            + String.format(
                                                            "%02d:%02d",
                                                            hourOfDay,
                                                            minute
                                                    );

                                            endTimeText.setText(text);
                                        },
                                        endCalendar.get(Calendar.HOUR_OF_DAY),
                                        endCalendar.get(Calendar.MINUTE),
                                        true
                                );

                        timePicker.show();
                    },
                    endCalendar.get(Calendar.YEAR),
                    endCalendar.get(Calendar.MONTH),
                    endCalendar.get(Calendar.DAY_OF_MONTH)
            );

            datePicker.show();
        });
    }
}
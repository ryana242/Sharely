package com.example.sharely;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.example.sharely.service.BookingService;

import com.example.sharely.data.BookingRepository;
import com.example.sharely.data.UserRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ItemDetailsActivity extends AppCompatActivity {

    private Calendar startCalendar;
    private Calendar endCalendar;

    private boolean startSelected = false;
    private boolean endSelected = false;
    private BookingRepository bookingRepository;
    private UserRepository userRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_item_details
        );

        // =====================================================
        //INITIAL REPOSITORY
        // =====================================================
        bookingRepository = new BookingRepository();

        userRepository = new UserRepository();

        // =====================================================
        // SYSTEM BAR SPACING
        // =====================================================

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.itemDetailsMain),

                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );


        // =====================================================
        // ITEM INFORMATION
        // =====================================================

        TextView itemName =
                findViewById(
                        R.id.textItemName
                );

        TextView itemPrice =
                findViewById(
                        R.id.textItemPrice
                );

        TextView itemInfo =
                findViewById(
                        R.id.textItemInfo
                );

        TextView itemDescription =
                findViewById(
                        R.id.textItemDescription
                );


        // =====================================================
        // PHOTO CONTAINER
        // =====================================================

        LinearLayout photoContainer =
                findViewById(
                        R.id.photoContainer
                );


        // =====================================================
        // GET ITEM DATA FROM INTENT
        // =====================================================

        Intent intent =
                getIntent();


        String itemId =
                intent.getStringExtra(
                        "itemId"
                );


        ArrayList<String> imageUrls =
                intent.getStringArrayListExtra(
                        "imageUrls"
                );


        itemName.setText(
                intent.getStringExtra(
                        "name"
                )
        );

        itemPrice.setText(
                intent.getStringExtra(
                        "price"
                )
        );

        itemInfo.setText(
                intent.getStringExtra(
                        "info"
                )
        );

        itemDescription.setText(
                intent.getStringExtra(
                        "description"
                )
        );


        // =====================================================
        // DISPLAY ITEM PHOTOS
        // =====================================================

        if (imageUrls != null
                && !imageUrls.isEmpty()) {

            for (String imageUrl : imageUrls) {

                ImageView imageView =
                        new ImageView(this);


                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                dpToPx(380),
                                LinearLayout.LayoutParams.MATCH_PARENT
                        );


                params.setMargins(
                        0,
                        0,
                        dpToPx(12),
                        0
                );


                imageView.setLayoutParams(
                        params
                );


                imageView.setScaleType(
                        ImageView.ScaleType.FIT_CENTER
                );


                Glide.with(this)
                        .load(imageUrl)
                        .fitCenter()
                        .into(imageView);


                photoContainer.addView(
                        imageView
                );
            }
        }


        // =====================================================
        // CALENDARS
        // =====================================================

        startCalendar =
                Calendar.getInstance();

        endCalendar =
                Calendar.getInstance();


        // =====================================================
        // TIME TEXT
        // =====================================================

        TextView startTimeText =
                findViewById(
                        R.id.textStartTime
                );

        TextView endTimeText =
                findViewById(
                        R.id.textEndTime
                );


        // =====================================================
        // BUTTONS
        // =====================================================

        Button backButton =
                findViewById(
                        R.id.buttonBack
                );

        Button selectStartButton =
                findViewById(
                        R.id.buttonSelectStart
                );

        Button selectEndButton =
                findViewById(
                        R.id.buttonSelectEnd
                );

        Button requestBookingButton =
                findViewById(
                        R.id.buttonRequestBooking
                );


        // =====================================================
        // BACK BUTTON
        // =====================================================

        backButton.setOnClickListener(v -> {

            finish();
        });


        // =====================================================
        // SELECT START DATE + TIME
        // =====================================================

        selectStartButton.setOnClickListener(v -> {

            DatePickerDialog datePicker =
                    new DatePickerDialog(

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

                                                (timeView,
                                                 hourOfDay,
                                                 minute) -> {

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

                                                    startCalendar.set(
                                                            Calendar.MILLISECOND,
                                                            0
                                                    );


                                                    startSelected =
                                                            true;


                                                    String text =
                                                            String.format(
                                                                    Locale.getDefault(),
                                                                    "Start: %02d/%02d/%04d %02d:%02d",
                                                                    dayOfMonth,
                                                                    month + 1,
                                                                    year,
                                                                    hourOfDay,
                                                                    minute
                                                            );


                                                    startTimeText.setText(
                                                            text
                                                    );
                                                },

                                                startCalendar.get(
                                                        Calendar.HOUR_OF_DAY
                                                ),

                                                startCalendar.get(
                                                        Calendar.MINUTE
                                                ),

                                                true
                                        );


                                timePicker.show();
                            },

                            startCalendar.get(
                                    Calendar.YEAR
                            ),

                            startCalendar.get(
                                    Calendar.MONTH
                            ),

                            startCalendar.get(
                                    Calendar.DAY_OF_MONTH
                            )
                    );


            datePicker.show();
        });


        // =====================================================
        // SELECT END DATE + TIME
        // =====================================================

        selectEndButton.setOnClickListener(v -> {

            DatePickerDialog datePicker =
                    new DatePickerDialog(

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


                                TimePickerDialog timePicker =
                                        new TimePickerDialog(

                                                ItemDetailsActivity.this,

                                                (timeView,
                                                 hourOfDay,
                                                 minute) -> {

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

                                                    endCalendar.set(
                                                            Calendar.MILLISECOND,
                                                            0
                                                    );


                                                    /*
                                                     * Only mark the end time
                                                     * as selected after the
                                                     * TimePicker is completed.
                                                     */
                                                    endSelected =
                                                            true;


                                                    String text =
                                                            String.format(
                                                                    Locale.getDefault(),
                                                                    "End: %02d/%02d/%04d %02d:%02d",
                                                                    dayOfMonth,
                                                                    month + 1,
                                                                    year,
                                                                    hourOfDay,
                                                                    minute
                                                            );


                                                    endTimeText.setText(
                                                            text
                                                    );
                                                },

                                                endCalendar.get(
                                                        Calendar.HOUR_OF_DAY
                                                ),

                                                endCalendar.get(
                                                        Calendar.MINUTE
                                                ),

                                                true
                                        );


                                timePicker.show();
                            },

                            endCalendar.get(
                                    Calendar.YEAR
                            ),

                            endCalendar.get(
                                    Calendar.MONTH
                            ),

                            endCalendar.get(
                                    Calendar.DAY_OF_MONTH
                            )
                    );


            datePicker.show();
        });


        // =====================================================
        // REQUEST BOOKING
        // =====================================================

        requestBookingButton.setOnClickListener(v -> {

            // -------------------------------------------------
            // BOTH TIMES MUST BE SELECTED
            // -------------------------------------------------

            if (!startSelected
                    || !endSelected) {

                Toast.makeText(
                        ItemDetailsActivity.this,
                        "Please select both start and end time",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            // -------------------------------------------------
            // CREATE REQUESTED SLOT
            // -------------------------------------------------

            AvailabilitySlot requestedSlot =
                    new AvailabilitySlot(

                            startCalendar
                                    .getTimeInMillis(),

                            endCalendar
                                    .getTimeInMillis()
                    );


            if (!requestedSlot.isValid()) {

                Toast.makeText(
                        ItemDetailsActivity.this,
                        "End time must be after start time",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            // -------------------------------------------------
            // GET CURRENT USER ID
            // -------------------------------------------------

            String borrowerId = userRepository.getCurrentUserId();
            if (borrowerId == null) {
                Toast.makeText(
                        ItemDetailsActivity.this,
                        "Please log in before requesting a booking",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            // -------------------------------------------------
            // GENERATE BOOKING ID
            // -------------------------------------------------

            String bookingId = bookingRepository.generateBookingId();

            // -------------------------------------------------
            // CREATE PENDING BOOKING
            // -------------------------------------------------

            Booking newBooking =
                    new Booking(
                            bookingId,
                            borrowerId,
                            itemId,
                            requestedSlot
                    );

            List<AvailabilitySlot> ownerAvailability =
                    createDemoAvailability();


            // =================================================
            // TEMPORARY EXISTING BOOKINGS
            // =================================================
            //
            // Later this will come from Leo's BookingRepository.
            // =================================================

            List<Booking> existingBookings =
                    new ArrayList<>();


            // -------------------------------------------------
            // VALIDATE REQUEST
            // -------------------------------------------------

            BookingService bookingService =
                    new BookingService();


            boolean canRequest =
                    bookingService.canRequestBooking(

                            newBooking,

                            ownerAvailability,

                            existingBookings
                    );


            // -------------------------------------------------
            // RESULT
            // -------------------------------------------------

            if (canRequest) {

                bookingRepository.addBooking(
                        newBooking,

                        new BookingRepository.OnBookingSavedListener() {

                            @Override
                            public void onSuccess() {

                                Toast.makeText(
                                        ItemDetailsActivity.this,
                                        "Booking request sent",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            @Override
                            public void onFailure(Exception e) {

                                Toast.makeText(
                                        ItemDetailsActivity.this,
                                        "Failed to send booking request",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );

            } else {

                Toast.makeText(
                        ItemDetailsActivity.this,
                        "Selected time is unavailable",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }


    // =========================================================
    // TEMPORARY OWNER AVAILABILITY
    // =========================================================

    private List<AvailabilitySlot> createDemoAvailability() {

        List<AvailabilitySlot> availability =
                new ArrayList<>();


        Calendar availableFrom =
                Calendar.getInstance();

        availableFrom.set(
                Calendar.SECOND,
                0
        );

        availableFrom.set(
                Calendar.MILLISECOND,
                0
        );


        Calendar availableUntil =
                (Calendar)
                        availableFrom.clone();

        availableUntil.add(
                Calendar.DAY_OF_MONTH,
                30
        );


        AvailabilitySlot slot =
                new AvailabilitySlot(

                        availableFrom
                                .getTimeInMillis(),

                        availableUntil
                                .getTimeInMillis()
                );


        availability.add(
                slot
        );


        return availability;
    }

    // =========================================================
    // DP TO PX
    // =========================================================

    private int dpToPx(int dp) {

        return (int) (
                dp
                        *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}
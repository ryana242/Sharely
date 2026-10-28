package com.example.sharely;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.sharely.data.ItemRepository;
import com.example.sharely.enums.ItemStatus;
import com.example.sharely.model.Item;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private ItemRepository itemRepository;
    private LinearLayout previewRow;
    private HorizontalScrollView previewScroller;
    private TextView previewStatus;

    public HomeFragment() {
        // Required empty constructor.
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.searchHome).setOnClickListener(v ->
                ((MainAppActivity) requireActivity()).openBrowse(true));

        view.findViewById(R.id.buttonListHome).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddItemActivity.class)));

        view.findViewById(R.id.buttonProfileHome).setOnClickListener(v ->
                ((MainAppActivity) requireActivity()).selectTab(R.id.navProfile));

        view.findViewById(R.id.homeSeeAll).setOnClickListener(v ->
                ((MainAppActivity) requireActivity()).openBrowse(false));

        itemRepository = new ItemRepository();
        previewRow = view.findViewById(R.id.homePreviewRow);
        previewScroller = view.findViewById(R.id.homePreviewScroller);
        previewStatus = view.findViewById(R.id.homePreviewStatus);

        loadPreviewItems(view);
    }

    private void loadPreviewItems(@NonNull View expectedView) {
        // Share the same repository and ACTIVE-status filtering used by Browse.
        itemRepository.getAllItems(new ItemRepository.OnItemsLoadedListener() {
            @Override
            public void onSuccess(List<Item> items) {
                if (!isAdded() || getView() != expectedView) return;

                previewRow.removeAllViews();
                int shown = 0;
                for (Item item : items) {
                    if (shown >= 6) break;
                    if (item == null || item.getStatus() != ItemStatus.ACTIVE
                            || item.getItemId() == null || item.getItemId().trim().isEmpty()
                            || item.getItemName() == null || item.getItemName().trim().isEmpty()) {
                        continue;
                    }
                    previewRow.addView(createItemCard(item));
                    shown++;
                }

                previewScroller.setVisibility(shown > 0 ? View.VISIBLE : View.GONE);
                previewStatus.setVisibility(shown > 0 ? View.GONE : View.VISIBLE);
                if (shown == 0) {
                    previewStatus.setText("No available listings yet. Check Browse again soon.");
                }
            }

            @Override
            public void onFailure(Exception error) {
                if (!isAdded() || getView() != expectedView) return;
                previewScroller.setVisibility(View.GONE);
                previewStatus.setVisibility(View.VISIBLE);
                previewStatus.setText("Couldn't load items right now. Browse to try again.");
            }
        });
    }

    private View createItemCard(@NonNull Item item) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(12));
        card.setBackgroundResource(R.drawable.bg_home_preview_v3);
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("View " + item.getItemName());

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(178),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        card.setMinimumHeight(dp(244));
        cardParams.setMargins(dp(2), dp(2), dp(13), dp(7));
        card.setLayoutParams(cardParams);

        ImageView image = new ImageView(requireContext());
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageResource(R.drawable.item_image_placeholder);
        GradientDrawable imageBackground = new GradientDrawable();
        imageBackground.setColor(Color.rgb(234, 245, 239));
        imageBackground.setCornerRadius(dp(13));
        image.setBackground(imageBackground);
        image.setClipToOutline(true);
        card.addView(image, new LinearLayout.LayoutParams(-1, dp(130)));

        if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()
                && item.getImageUrls().get(0) != null) {
            Glide.with(this)
                    .load(item.getImageUrls().get(0))
                    .centerCrop()
                    .placeholder(R.drawable.item_image_placeholder)
                    .into(image);
        }

        TextView name = addText(card, item.getItemName(), 15, 0xFF123D31, true, 10);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);

        String place = item.getLocation();
        TextView location = addText(card,
                place == null || place.trim().isEmpty() ? "Location not set" : place,
                12, 0xFF5A7D72, false, 3);
        location.setSingleLine(true);
        location.setEllipsize(android.text.TextUtils.TruncateAt.END);

        addText(card, String.format(Locale.US, "$%.2f / day", item.getPrice()),
                14, 0xFF176B52, true, 7);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ItemDetailsActivity.class);
            intent.putExtra("itemId", item.getItemId());
            startActivity(intent);
        });
        return card;
    }

    private TextView addText(LinearLayout parent, String text, int sp,
                             int color, boolean bold, int topMarginDp) {
        TextView label = new TextView(requireContext());
        label.setText(text);
        label.setTextSize(sp);
        label.setTextColor(color);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(topMarginDp);
        parent.addView(label, params);
        return label;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

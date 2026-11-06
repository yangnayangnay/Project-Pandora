package com.example.project_pandora.ui.mine;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.project_pandora.R;
import com.example.project_pandora.core.network.NetworkClient;
import com.example.project_pandora.core.network.TokenManager;
import com.example.project_pandora.core.util.AvatarUploadPolicy;

import java.util.function.Consumer;

/** Glide requests do not pass through Retrofit's authentication interceptor. */
public final class AvatarLoader {
    private AvatarLoader() {}

    public static void load(ImageView target, String avatar) { load(target, avatar, false, null); }

    public static void load(ImageView target, String avatar, boolean fresh, @Nullable Consumer<Boolean> displayed) {
        String base = NetworkClient.getBaseUrl();
        String url = AvatarUploadPolicy.resolveUrl(base, avatar);
        Object model = url;
        if (AvatarUploadPolicy.requiresAuthentication(base, url)) {
            String token = TokenManager.getInstance(target.getContext()).getToken();
            if (token != null && !token.isEmpty()) model = new GlideUrl(url,
                    new LazyHeaders.Builder().addHeader("Authorization", "Bearer " + token).build());
        }
        RequestBuilder<Drawable> request = Glide.with(target).load(model).circleCrop()
                .placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder);
        if (fresh) request = request.skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE);
        if (displayed != null) request = request.listener(new RequestListener<Drawable>() {
            @Override public boolean onLoadFailed(@Nullable GlideException error, Object model,
                                                  Target<Drawable> target, boolean first) {
                displayed.accept(false);
                return false;
            }
            @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                                      DataSource source, boolean first) {
                displayed.accept(true);
                return false;
            }
        });
        request.into(target);
    }
}

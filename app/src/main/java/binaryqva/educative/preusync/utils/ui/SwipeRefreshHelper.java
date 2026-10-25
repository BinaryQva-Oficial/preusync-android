/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: SwipeRefreshHelper.java
 * Versión: v1.3.0
 * Descripción: Ayudante para la personalización de SwipeRefreshLayout.
 *              Mantiene el indicador dinámico visible durante todo el refresco.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.utils.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.lang.reflect.Field;

public class SwipeRefreshHelper implements ViewTreeObserver.OnPreDrawListener {

    private final SwipeRefreshLayout swipeRefreshLayout;
    private final Context context;
    private final int colorAccent;

    private CustomSwipeRefreshDrawable customDrawable;
    private ImageView circleImageView;
    private int originalOffsetTop, spinnerOffsetEnd;

    private final Handler postRefreshHandler = new Handler(Looper.getMainLooper());
    private Runnable postRefreshRunnable;

    private OnRefreshListener onRefreshListener;

    public interface OnRefreshListener { void onRefresh(); }

    public SwipeRefreshHelper(SwipeRefreshLayout swipeRefreshLayout, int colorAccent) {
        this.swipeRefreshLayout = swipeRefreshLayout;
        this.context = swipeRefreshLayout.getContext();
        this.colorAccent = colorAccent;
        initialize();
    }

    private void initialize() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            startRefreshingAnimation();
            if (onRefreshListener != null) onRefreshListener.onRefresh();
        });

        // Configuración en diferido para asegurar que la jerarquía esté inflada.
        swipeRefreshLayout.post(() -> {
            personalizeIndicator();
            initCircleViewReferences();
            if (circleImageView != null) circleImageView.getViewTreeObserver().addOnPreDrawListener(this);
        });
    }

    public void setOnRefreshListener(OnRefreshListener l) { this.onRefreshListener = l; }

    /**
     * Inyecta el drawable personalizado dentro del CircleView de la librería.
     */
    private void personalizeIndicator() {
        try {
            Field f = SwipeRefreshLayout.class.getDeclaredField("mCircleView");
            f.setAccessible(true);
            Object v = f.get(swipeRefreshLayout);

            if (v instanceof ImageView) {
                ImageView iv = (ImageView) v;
                customDrawable = new CustomSwipeRefreshDrawable(context, colorAccent, 4f, 24f);
                iv.setImageDrawable(customDrawable);
                iv.setPadding(0, 0, 0, 0); iv.setScaleType(ImageView.ScaleType.CENTER);
                iv.setBackgroundColor(Color.TRANSPARENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ViewCompat.setElevation(iv, 0f);
            }
        } catch (Exception ignored) {}
    }

    private void initCircleViewReferences() {
        try {
            Field cF = SwipeRefreshLayout.class.getDeclaredField("mCircleView"); cF.setAccessible(true);
            circleImageView = (ImageView) cF.get(swipeRefreshLayout);
            Field oF = SwipeRefreshLayout.class.getDeclaredField("mOriginalOffsetTop"); oF.setAccessible(true);
            originalOffsetTop = oF.getInt(swipeRefreshLayout);
            Field eF = SwipeRefreshLayout.class.getDeclaredField("mSpinnerOffsetEnd"); eF.setAccessible(true);
            spinnerOffsetEnd = eF.getInt(swipeRefreshLayout);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onPreDraw() {
        if (!swipeRefreshLayout.isRefreshing() && customDrawable != null && circleImageView != null) {
            int max = spinnerOffsetEnd - originalOffsetTop;
            if (max > 0) customDrawable.setDragProgress(Math.max(0f, Math.min(1f, (float) (circleImageView.getTop() - originalOffsetTop) / max)));
        }
        return true;
    }

    public void startRefreshingAnimation() {
        if (customDrawable != null) {
            customDrawable.resetAlpha();
            customDrawable.startRefreshingAnimation();
        }
    }

    /**
     * Concluye el estado de refresco y reinicia el indicador visual.
     */
    public void finishRefresh() {
        swipeRefreshLayout.setRefreshing(false);
        if (postRefreshRunnable != null) postRefreshHandler.removeCallbacks(postRefreshRunnable);
        postRefreshRunnable = () -> {
            if (customDrawable != null) {
                customDrawable.stopRefreshingAnimation();
                customDrawable.resetAlpha();
            }
        };
        postRefreshHandler.postDelayed(postRefreshRunnable, 200);
    }

    public void cleanup() {
        if (circleImageView != null) circleImageView.getViewTreeObserver().removeOnPreDrawListener(this);
        if (postRefreshRunnable != null) postRefreshHandler.removeCallbacks(postRefreshRunnable);
    }
}

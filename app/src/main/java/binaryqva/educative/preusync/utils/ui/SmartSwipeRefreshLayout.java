/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: SmartSwipeRefreshLayout.java
 * Versión: v4.2.0
 * Descripción: Layout de refresco táctil optimizado para interceptar gestos
 *              verticales sin interferir con ViewPager2 ni ScrollViews horizontales.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.utils.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class SmartSwipeRefreshLayout extends CustomSwipeRefreshLayout {

    private static final String TAG = "SmartSwipeRefresh";

    private View targetScrollableView;
    private int touchSlop;
    private float startX, startY;
    private boolean isDragging;

    public SmartSwipeRefreshLayout(Context context) { super(context); init(context); }
    public SmartSwipeRefreshLayout(Context context, AttributeSet attrs) { super(context, attrs); init(context); }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * Vincula el componente de scroll objetivo (RecyclerView/ScrollView)
     * para consultar su capacidad de desplazamiento ascendente.
     */
    public void setTargetScrollableView(View v) { this.targetScrollableView = v; }
    public void setTargetRecyclerView(View v) { this.targetScrollableView = v; }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (targetScrollableView == null) return super.onInterceptTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX(); startY = ev.getY(); isDragging = false;
                super.onInterceptTouchEvent(ev);
                return false;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = ev.getY() - startY;

                if (dy > touchSlop) isDragging = true;

                if (isDragging) {
                    // Intercepta si el gesto es vertical descendente y el ScrollView está en el tope
                    if (dy > dx * 1.2f && !targetScrollableView.canScrollVertically(-1)) {
                        return super.onInterceptTouchEvent(ev);
                    }
                    return false;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
}

/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase Abstracta: BaseFragment.java
 * Versión: v2.1.0
 * Descripción: Clase base unificada para TODOS los fragmentos. Proporciona
 *              gestión centralizada de los 4 estados visuales (Loading,
 *              Content, Empty, Error) e interactividad de reintentos.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */
package binaryqva.educative.preusync.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.debug.AppLogger;

/**
 * Clase base para todos los fragmentos de la aplicación PreuSync.
 * Centraliza la inicialización de vistas, observadores y estados de UI.
 */
public abstract class BaseFragment extends Fragment {

    protected final String TAG = "PreuSync_" + getClass().getSimpleName();

    public static final int STATE_LOADING = 0;
    public static final int STATE_CONTENT = 1;
    public static final int STATE_EMPTY = 2;
    public static final int STATE_ERROR = 3;

    protected View loaderLayout;
    protected View emptyLayout;
    protected View errorLayout;
    protected View contentView;

    /**
     * @return El ID del recurso de layout para inflar.
     */
    @LayoutRes
    protected abstract int getLayoutResId();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        AppLogger.d(TAG, "onCreateView");
        return inflater.inflate(getLayoutResId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppLogger.d(TAG, "onViewCreated");

        // Búsqueda estándar de vistas de estado en el layout inflado
        loaderLayout = view.findViewById(R.id.loaderLayout);
        if (loaderLayout == null) loaderLayout = view.findViewById(R.id.profileLoader);

        emptyLayout = view.findViewById(R.id.emptyLayout);

        errorLayout = view.findViewById(R.id.errorLayout);
        if (errorLayout == null) errorLayout = view.findViewById(R.id.profileErrorLayout);

        contentView = view.findViewById(R.id.recyclerView);
        if (contentView == null) contentView = view.findViewById(R.id.profileRecyclerView);
        if (contentView == null) contentView = view.findViewById(R.id.viewFlipper);

        setupBaseStateInteractions(view);
        setupViews(view);
        setupObservers();
    }

    /**
     * Configura la interactividad en las vistas de error y estado vacío.
     */
    protected void setupBaseStateInteractions(View view) {
        View.OnClickListener retryListener = v -> onRetry();

        if (errorLayout != null) {
            errorLayout.setOnClickListener(retryListener);
            View retryBtn = errorLayout.findViewById(R.id.retryButton);
            if (retryBtn == null) retryBtn = errorLayout.findViewById(R.id.profileRetryButton);
            if (retryBtn != null) retryBtn.setOnClickListener(retryListener);
        }
        if (emptyLayout != null) {
            emptyLayout.setOnClickListener(retryListener);
        }
        View genericRetryBtn = view.findViewById(R.id.retryButton);
        if (genericRetryBtn == null) genericRetryBtn = view.findViewById(R.id.profileRetryButton);
        if (genericRetryBtn != null) {
            genericRetryBtn.setOnClickListener(retryListener);
        }
    }

    /**
     * Acción invocada al presionar la pantalla de error o el botón de reintento.
     * Sobrescribir en fragmentos específicos según sea necesario.
     */
    protected void onRetry() {
        // Implementación por defecto
    }

    /**
     * Controla la conmutación de estados de UI de forma unificada para toda la App.
     * @param state Estado objetivo (STATE_LOADING, STATE_EMPTY, STATE_ERROR, STATE_CONTENT).
     * @param hasData Indica si existen datos cargados en memoria/pantalla.
     */
    protected void updateUiState(int state, boolean hasData) {
        boolean showContent = (state == STATE_CONTENT) || (hasData && (state == STATE_LOADING || state == STATE_ERROR));
        boolean showLoader = (state == STATE_LOADING && !hasData);
        boolean showEmpty = (state == STATE_EMPTY) || (state == STATE_CONTENT && !hasData);
        boolean showError = (state == STATE_ERROR && !hasData);

        if (contentView != null) contentView.setVisibility(showContent ? View.VISIBLE : View.GONE);
        if (loaderLayout != null) loaderLayout.setVisibility(showLoader ? View.VISIBLE : View.GONE);
        if (emptyLayout != null) emptyLayout.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        if (errorLayout != null) errorLayout.setVisibility(showError ? View.VISIBLE : View.GONE);
    }

    /**
     * Inicializa las vistas del fragmento. Se llama en onViewCreated.
     * @param view La vista raíz del fragmento.
     */
    protected abstract void setupViews(View view);

    /**
     * Configura los observadores de LiveData del ViewModel.
     */
    protected void setupObservers() {
        // Implementación opcional
    }

    /**
     * Muestra u oculta un estado de carga.
     * @param show true para mostrar, false para ocultar.
     */
    protected void showLoading(boolean show) {
        updateUiState(show ? STATE_LOADING : STATE_CONTENT, false);
    }

    /**
     * Muestra un mensaje de error al usuario.
     * @param message El mensaje de error.
     */
    protected void showError(String message) {
        updateUiState(STATE_ERROR, false);
    }
}

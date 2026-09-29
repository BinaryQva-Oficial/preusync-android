/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: HomeViewModel.java
 * Versión: v10.1.0
 * Descripción: ViewModel central para el inicio. Coordina la carga de noticias,
 *              posts, efemérides y turnos con estados visuales robustos.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import binaryqva.educative.preusync.data.repositories.EphemerisRepository;
import binaryqva.educative.preusync.data.repositories.NewsRepository;
import binaryqva.educative.preusync.data.repositories.PostRepository;
import binaryqva.educative.preusync.data.repositories.ScheduleRepository;
import binaryqva.educative.preusync.data.repositories.VoteRepository;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.Ephemeris;
import binaryqva.educative.preusync.network.models.News;
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.network.models.Schedule;
import binaryqva.educative.preusync.utils.common.AppUtils;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "PreuSync_HomeVM";

    public static final int STATE_LOADING = 0;
    public static final int STATE_SUCCESS = 1;
    public static final int STATE_EMPTY = 2;
    public static final int STATE_ERROR = 3;

    private final NewsRepository newsRepository;
    private final PostRepository postRepository;
    private final EphemerisRepository ephemerisRepository;
    private final ScheduleRepository scheduleRepository;
    private final VoteRepository voteRepository;
    private final PreferenceManager preferenceManager;

    private final MutableLiveData<List<News>> newsList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Post>> postsList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Ephemeris> ephemerisData = new MutableLiveData<>(null);
    private final MutableLiveData<ShiftData> shiftData = new MutableLiveData<>(new ShiftData("", "", ""));
    
    private final MutableLiveData<Integer> newsState = new MutableLiveData<>(STATE_LOADING);
    private final MutableLiveData<Integer> postsState = new MutableLiveData<>(STATE_LOADING);
    private final MutableLiveData<Integer> ephemerisState = new MutableLiveData<>(STATE_LOADING);
    private final MutableLiveData<Integer> shiftState = new MutableLiveData<>(STATE_LOADING);

    private final MutableLiveData<Set<String>> processingPosts = new MutableLiveData<>(new HashSet<>());

    public static class ShiftData {
        public final String shiftNumber; public final String timeRange; public final String subject;
        public ShiftData(String s, String t, String sub) { this.shiftNumber = s; this.timeRange = t; this.subject = sub; }
    }

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.newsRepository = new NewsRepository(application);
        this.postRepository = new PostRepository(application);
        this.ephemerisRepository = new EphemerisRepository(application);
        this.scheduleRepository = new ScheduleRepository(application);
        this.voteRepository = VoteRepository.getInstance(application);
        this.preferenceManager = PreferenceManager.getInstance(application);
    }

    public LiveData<List<News>> getNewsList() { return newsList; }
    public LiveData<List<Post>> getPostsList() { return postsList; }
    public LiveData<Ephemeris> getEphemerisData() { return ephemerisData; }
    public LiveData<ShiftData> getShiftData() { return shiftData; }
    
    public LiveData<Integer> getNewsState() { return newsState; }
    public LiveData<Integer> getPostsState() { return postsState; }
    public LiveData<Integer> getEphemerisState() { return ephemerisState; }
    public LiveData<Integer> getShiftState() { return shiftState; }

    public LiveData<Map<String, Integer>> getVoteStates() { return voteRepository.getVoteStates(); }
    public LiveData<Map<String, VoteRepository.PostCounts>> getPostCounts() { return voteRepository.getPostCounts(); }
    public LiveData<Set<String>> getProcessingPosts() { return processingPosts; }

    public void initVoteRepository(String userId) {
        if (userId != null && !userId.isEmpty()) { voteRepository.init(userId); voteRepository.loadVotesFromPrefs(); }
    }

    public void loadHomeData() {
        AppLogger.d(TAG, "loadHomeData: Cargando datos principales de Home...");
        if (!AppUtils.isConnected(getApplication())) {
            AppLogger.w(TAG, "loadHomeData: Sin conexión a internet, consultando datos en Room...");
            loadFromRoom();
            return;
        }
        loadTopNews(); loadTopPosts(); loadEphemeris(); loadSchedule();
    }

    private void loadFromRoom() {
        newsRepository.getLocalNews(data -> {
            if (data != null && !data.isEmpty()) { newsList.setValue(data); newsState.setValue(STATE_SUCCESS); }
            else { newsState.setValue(STATE_EMPTY); }
        });
        postRepository.getLocalPosts(data -> {
            if (data != null && !data.isEmpty()) { syncVotesAndCounts(data); postsList.setValue(data); postsState.setValue(STATE_SUCCESS); }
            else { postsState.setValue(STATE_EMPTY); }
        });
    }

    private void loadTopNews() {
        newsState.setValue(STATE_LOADING);
        newsRepository.getTopNews(5, new Callback<ApiResponse<List<News>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<News>>> call, @NonNull Response<ApiResponse<List<News>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<News> data = response.body().getData();
                    List<News> items = data != null ? data : new ArrayList<>();
                    newsList.setValue(items);
                    newsState.setValue(items.isEmpty() ? STATE_EMPTY : STATE_SUCCESS);
                    AppLogger.d(TAG, "loadTopNews: " + items.size() + " noticias. Estado: " + (items.isEmpty() ? "EMPTY" : "SUCCESS"));
                } else {
                    AppLogger.w(TAG, "loadTopNews: HTTP " + response.code() + ", buscando en Room local...");
                    newsRepository.getLocalNews(localData -> {
                        if (localData != null && !localData.isEmpty()) {
                            newsList.setValue(localData);
                            newsState.setValue(STATE_SUCCESS);
                        } else {
                            newsState.setValue(STATE_EMPTY);
                        }
                    });
                }
            }

            @Override public void onFailure(@NonNull Call<ApiResponse<List<News>>> call, @NonNull Throwable t) {
                AppLogger.e(TAG, "loadTopNews onFailure: " + t.getMessage(), t);
                newsRepository.getLocalNews(localData -> {
                    if (localData != null && !localData.isEmpty()) {
                        newsList.setValue(localData);
                        newsState.setValue(STATE_SUCCESS);
                    } else {
                        newsState.setValue(STATE_EMPTY);
                    }
                });
            }
        });
    }

    private void loadTopPosts() {
        postsState.setValue(STATE_LOADING);
        postRepository.getTopPosts(5, new Callback<ApiResponse<List<Post>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Post>>> call, @NonNull Response<ApiResponse<List<Post>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Post> data = response.body().getData();
                    List<Post> items = data != null ? data : new ArrayList<>();
                    syncVotesAndCounts(items);
                    postsList.setValue(items);
                    postsState.setValue(items.isEmpty() ? STATE_EMPTY : STATE_SUCCESS);
                    AppLogger.d(TAG, "loadTopPosts: " + items.size() + " posts. Estado: " + (items.isEmpty() ? "EMPTY" : "SUCCESS"));
                } else {
                    AppLogger.w(TAG, "loadTopPosts: HTTP " + response.code() + ", buscando en Room local...");
                    postRepository.getLocalPosts(localData -> {
                        if (localData != null && !localData.isEmpty()) {
                            syncVotesAndCounts(localData);
                            postsList.setValue(localData);
                            postsState.setValue(STATE_SUCCESS);
                        } else {
                            postsState.setValue(STATE_EMPTY);
                        }
                    });
                }
            }

            @Override public void onFailure(@NonNull Call<ApiResponse<List<Post>>> call, @NonNull Throwable t) {
                AppLogger.e(TAG, "loadTopPosts onFailure: " + t.getMessage(), t);
                postRepository.getLocalPosts(localData -> {
                    if (localData != null && !localData.isEmpty()) {
                        syncVotesAndCounts(localData);
                        postsList.setValue(localData);
                        postsState.setValue(STATE_SUCCESS);
                    } else {
                        postsState.setValue(STATE_EMPTY);
                    }
                });
            }
        });
    }

    private void syncVotesAndCounts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return;
        Map<String, Integer> votes = new HashMap<>();
        Map<String, VoteRepository.PostCounts> counts = new HashMap<>();
        for (Post p : posts) {
            if (p == null || p.getId() == null) continue;
            int voteVal = 0;
            if ("up".equals(p.getUserVote())) voteVal = 1;
            else if ("down".equals(p.getUserVote())) voteVal = 2;
            votes.put(p.getId(), voteVal);
            counts.put(p.getId(), new VoteRepository.PostCounts(p.getLikes(), p.getDislikes()));
        }
        voteRepository.setPostCountsMap(counts);
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            voteRepository.setVoteState(entry.getKey(), entry.getValue());
        }
    }

    private void loadEphemeris() {
        ephemerisState.setValue(STATE_LOADING);
        String today = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(Calendar.getInstance().getTime());
        ephemerisRepository.getEphemeris(today, new Callback<ApiResponse<List<Ephemeris>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Ephemeris>>> call, @NonNull Response<ApiResponse<List<Ephemeris>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Ephemeris> data = response.body().getData();
                    if (data != null && !data.isEmpty()) {
                        ephemerisData.setValue(data.get(0));
                        ephemerisState.setValue(STATE_SUCCESS);
                    } else {
                        ephemerisData.setValue(null);
                        ephemerisState.setValue(STATE_EMPTY);
                    }
                } else ephemerisState.setValue(STATE_EMPTY);
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Ephemeris>>> call, @NonNull Throwable t) { ephemerisState.setValue(STATE_EMPTY); }
        });
    }

    private void loadSchedule() {
        String group = preferenceManager.getUserGroup();
        String schoolId = preferenceManager.getSchoolId();
        if (group == null || group.isEmpty() || schoolId == null) { shiftState.setValue(STATE_EMPTY); return; }
        shiftState.setValue(STATE_LOADING);
        scheduleRepository.getSchedule(group, schoolId, new Callback<ApiResponse<List<Schedule>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Schedule>>> call, @NonNull Response<ApiResponse<List<Schedule>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    processScheduleData(response.body().getData());
                } else shiftState.setValue(STATE_EMPTY);
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Schedule>>> call, @NonNull Throwable t) { shiftState.setValue(STATE_EMPTY); }
        });
    }

    private void processScheduleData(List<Schedule> scheduleRows) {
        if (scheduleRows == null || scheduleRows.isEmpty()) {
            shiftData.setValue(new ShiftData("", "", "Libre"));
            shiftState.setValue(STATE_EMPTY);
            return;
        }
        Calendar cal = Calendar.getInstance();
        int now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        String day = getCurrentDayLetter(cal);
        
        String[] ranges = new String[]{"07:40-08:25", "08:30-09:15", "09:20-10:05", "10:25-11:10", "11:15-12:00", "13:00-13:45", "13:50-14:35", "14:40-15:25"};

        for (int i = 0; i < ranges.length; i++) {
            String[] p = ranges[i].split("-");
            if (now >= timeToMin(p[0]) && now < timeToMin(p[1])) {
                String subject = "Libre";
                for (Schedule s : scheduleRows) {
                    if (day.equals(s.getDay()) && s.getShift() == (i + 1)) {
                        subject = s.getSubject(); break;
                    }
                }
                shiftData.setValue(new ShiftData(String.valueOf(i + 1), ranges[i], subject));
                shiftState.setValue(STATE_SUCCESS);
                return;
            }
        }
        shiftData.setValue(new ShiftData("", "", "Libre"));
        shiftState.setValue(STATE_SUCCESS);
    }

    private int timeToMin(String t) { String[] p = t.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); }

    private String getCurrentDayLetter(Calendar cal) {
        switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY: return "L"; case Calendar.TUESDAY: return "M";
            case Calendar.WEDNESDAY: return "X"; case Calendar.THURSDAY: return "J";
            case Calendar.FRIDAY: return "V"; default: return "L";
        }
    }
}

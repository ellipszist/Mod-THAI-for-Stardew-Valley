package com.zane.smapiinstaller.logic;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Queues;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.Response;
import com.microsoft.appcenter.crashes.Crashes;
import com.zane.smapiinstaller.R;
import com.zane.smapiinstaller.constant.Constants;
import com.zane.smapiinstaller.constant.DialogAction;
import com.zane.smapiinstaller.dto.ModUpdateCheckRequestDto;
import com.zane.smapiinstaller.dto.ModUpdateCheckResponseDto;
import com.zane.smapiinstaller.entity.ModManifestEntry;
import com.zane.smapiinstaller.utils.DialogUtils;
import com.zane.smapiinstaller.utils.FileUtils;
import com.zane.smapiinstaller.utils.JsonUtil;
import com.zane.smapiinstaller.utils.JsonCallback;
import com.zane.smapiinstaller.utils.VersionUtil;

import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Mod资源管理器
 * @author Zane
 */
public class ModAssetsManager {

    private final View root;

    private static final String TAG = "MANAGER";

    private final AtomicBoolean checkUpdating = new AtomicBoolean(false);

    public ModAssetsManager(View root) {
        this.root = root;
    }

    /**
     * 查找第一个匹配的Mod
     *
     * @param filter 过滤规则
     * @return Mod信息
     */
    public static ModManifestEntry findFirstModIf(Predicate<ModManifestEntry> filter) {
        ConcurrentLinkedQueue<File> files = Queues.newConcurrentLinkedQueue();
        files.add(new File(Environment.getExternalStorageDirectory(), Constants.MOD_PATH));
        do {
            File currentFile = files.poll();
            if (currentFile != null && currentFile.exists()) {
                boolean foundManifest = false;
                File[] listFiles = currentFile.listFiles(File::isFile);
                if (listFiles == null) {
                    continue;
                }
                for (File file : listFiles) {
                    if (StringUtils.equalsIgnoreCase(file.getName(), "manifest.json")) {
                        ModManifestEntry manifest = FileUtils.getFileJson(file, ModManifestEntry.class);
                        foundManifest = true;
                        if (manifest != null) {
                            manifest.setAssetPath(file.getParentFile().getAbsolutePath());
                            if (filter.test(manifest)) {
                                return manifest;
                            }
                        }
                        break;
                    }
                }
                if (!foundManifest) {
                    files.addAll(Lists.newArrayList(currentFile.listFiles(File::isDirectory)));
                }
            }
        } while (!files.isEmpty());
        return null;
    }

    /**
     * 查找全部已识别Mod
     *
     * @return Mod信息列表
     */
    public static List<ModManifestEntry> findAllInstalledMods() {
        return findAllInstalledMods(false);
    }

    /**
     * 查找全部已识别Mod
     *
     * @param ignoreDisabledMod 是否忽略禁用的mod
     * @return Mod信息列表
     */
    public static List<ModManifestEntry> findAllInstalledMods(boolean ignoreDisabledMod) {
        ConcurrentLinkedQueue<File> files = Queues.newConcurrentLinkedQueue();
        files.add(new File(Environment.getExternalStorageDirectory(), Constants.MOD_PATH));
        List<ModManifestEntry> mods = new ArrayList<>(30);
        do {
            File currentFile = files.poll();
            if (currentFile != null && currentFile.exists()) {
                boolean foundManifest = false;
                File[] listFiles = currentFile.listFiles(File::isFile);
                if (listFiles != null) {
                    for (File file : listFiles) {
                        if (StringUtils.equalsIgnoreCase(file.getName(), "manifest.json")) {
                            ModManifestEntry manifest = FileUtils.getFileJson(file, ModManifestEntry.class);
                            foundManifest = true;
                            if (manifest != null && StringUtils.isNoneBlank(manifest.getUniqueID())) {
                                if (ignoreDisabledMod && StringUtils.startsWith(file.getParentFile().getName(), ".")) {
                                    break;
                                }
                                manifest.setAssetPath(file.getParentFile().getAbsolutePath());
                                manifest.setLastModified(file.lastModified());
                                mods.add(manifest);
                            }
                            break;
                        }
                    }
                }
                if (!foundManifest) {
                    File[] listDirectories = currentFile.listFiles(File::isDirectory);
                    if (listDirectories != null) {
                        files.addAll(Lists.newArrayList(listDirectories));
                    }
                }
            }
        } while (!files.isEmpty());
        return mods;
    }

    /**
     * 安装默认Mod
     *
     * @return 是否安装成功
     */
    public boolean installDefaultMods() {
        Activity context = CommonLogic.getActivityFromView(root);
        List<ModManifestEntry> modManifestEntries = FileUtils.getAssetJson(context, "mods_manifest.json", new TypeReference<List<ModManifestEntry>>() {
        });
        if (modManifestEntries == null) {
            return false;
        }
        File modFolder = new File(Environment.getExternalStorageDirectory(), Constants.MOD_PATH);
        ImmutableListMultimap<String, ModManifestEntry> installedModMap = Multimaps.index(findAllInstalledMods(), ModManifestEntry::getUniqueID);
        for (ModManifestEntry mod : modManifestEntries) {
            if (installedModMap.containsKey(mod.getUniqueID()) || installedModMap.containsKey(mod.getUniqueID().replace("ZaneYork.CustomLocalization", "SMAPI.CustomLocalization"))) {
                ImmutableList<ModManifestEntry> installedMods = installedModMap.get(mod.getUniqueID());
                if (installedMods.size() > 1) {
                    DialogUtils.showAlertDialog(root, R.string.error,
                            String.format(context.getString(R.string.duplicate_mod_found),
                                    installedMods.stream().map(item -> FileUtils.toPrettyPath(item.getAssetPath())).collect(Collectors.joining(","))));
                    return false;
                } else if (installedMods.size() == 0) {
                    installedMods = installedModMap.get(mod.getUniqueID().replace("ZaneYork.CustomLocalization", "SMAPI.CustomLocalization"));
                }
                if (installedMods.size() > 0) {
                    try {
                        ZipUtil.unpack(context.getAssets().open(mod.getAssetPath()), new File(installedMods.get(0).getAssetPath()), (name) -> StringUtils.removeStart(name, mod.getName() + "/"));
                    } catch (IOException e) {
                        Log.e(TAG, "Install Mod Error", e);
                    }
                }
            } else {
                try {
                    ZipUtil.unpack(context.getAssets().open(mod.getAssetPath()), modFolder);
                } catch (IOException e) {
                    Log.e(TAG, "Install Mod Error", e);
                }
            }
        }
        return true;
    }

    /**
     * 检查Mod环境
     *
     * @param returnCallback 回调函数
     */
    public void checkModEnvironment(Consumer<Boolean> returnCallback) {
        ImmutableListMultimap<String, ModManifestEntry> installedModMap = Multimaps.index(findAllInstalledMods(true), ModManifestEntry::getUniqueID);
        checkDuplicateMod(installedModMap, (isConfirm) -> {
            if (isConfirm) {
                checkUnsatisfiedDependencies(installedModMap, (isConfirm2) -> {
                    if (isConfirm2) {
                        checkContentpacks(installedModMap, returnCallback);
                    } else {
                        returnCallback.accept(false);
                    }
                });
            } else {
                returnCallback.accept(false);
            }
        });
    }

    /**
     * 检查是否有重复Mod
     *
     * @param installedModMap 已安装Mod集合
     * @param returnCallback  回调函数
     */
    private void checkDuplicateMod(ImmutableListMultimap<String, ModManifestEntry> installedModMap, Consumer<Boolean> returnCallback) {
        // Duplicate mod check
        ArrayList<String> list = Lists.newArrayList();
        for (String key : installedModMap.keySet()) {
            ImmutableList<ModManifestEntry> installedMods = installedModMap.get(key);
            if (installedMods.size() > 1) {
                list.add(installedMods.stream().map(item -> FileUtils.toPrettyPath(item.getAssetPath())).collect(Collectors.joining(",")));
            }
        }
        if (!list.isEmpty()) {
            DialogUtils.showConfirmDialog(root, R.string.error,
                    root.getContext().getString(R.string.duplicate_mod_found, Joiner.on(";").join(list)),
                    R.string.continue_text, R.string.abort,
                    ((dialog, which) -> {
                        if (which == DialogAction.POSITIVE) {
                            returnCallback.accept(true);
                        } else {
                            returnCallback.accept(false);
                        }
                    }));
        } else {
            returnCallback.accept(true);
        }
    }

    /**
     * 检查是否有依赖关系缺失
     *
     * @param installedModMap 已安装Mod集合
     * @param returnCallback  回调函数
     */
    private void checkUnsatisfiedDependencies(ImmutableListMultimap<String, ModManifestEntry> installedModMap, Consumer<Boolean> returnCallback) {
        List<String> dependencyErrors = installedModMap.values().stream()
                .map(mod -> checkModDependencyError(mod, installedModMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (dependencyErrors.size() > 0) {
            DialogUtils.showConfirmDialog(root, R.string.error,
                    Joiner.on(";").join(dependencyErrors),
                    R.string.continue_text, R.string.abort,
                    ((dialog, which) -> {
                        if (which == DialogAction.POSITIVE) {
                            returnCallback.accept(true);
                        } else {
                            returnCallback.accept(false);
                        }
                    }));
        } else {
            returnCallback.accept(true);
        }
    }

    /**
     * 检查是否有资源包依赖Mod没有安装
     *
     * @param installedModMap 已安装Mod集合
     * @param returnCallback  回调函数
     */
    private void checkContentpacks(ImmutableListMultimap<String, ModManifestEntry> installedModMap, Consumer<Boolean> returnCallback) {
        List<String> dependencyErrors = installedModMap.values().stream()
                .map(mod -> checkContentPackDependencyError(mod, installedModMap))
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (!dependencyErrors.isEmpty()) {
            DialogUtils.showConfirmDialog(root, R.string.error,
                    Joiner.on(";").join(dependencyErrors),
                    R.string.continue_text, R.string.abort,
                    ((dialog, which) -> {
                        if (which == DialogAction.POSITIVE) {
                            returnCallback.accept(true);
                        } else {
                            returnCallback.accept(false);
                        }
                    }));
        } else {
            returnCallback.accept(true);
        }
    }

    public void checkModUpdate(Consumer<List<ModUpdateCheckResponseDto>> callback) {
        if (checkUpdating.get()) {
            return;
        }
        List<ModUpdateCheckRequestDto.ModInfo> list = findAllInstalledMods(false).stream()
                .filter(mod -> mod.getUpdateKeys() != null && !mod.getUpdateKeys().isEmpty())
                .map(ModUpdateCheckRequestDto.ModInfo::fromModManifestEntry)
                .distinct()
                .filter(modInfo -> modInfo.getInstalledVersion() != null)
                .collect(Collectors.toList());
        Activity context = CommonLogic.getActivityFromView(root);
        PackageInfo gamePackageInfo = GameLauncher.getGamePackageInfo(context);
        if (gamePackageInfo == null) {
            return;
        }
        context.runOnUiThread(() -> Toast.makeText(context, R.string.mod_version_update_checking, Toast.LENGTH_SHORT).show());
        checkUpdating.set(true);
        try {
            ModUpdateCheckRequestDto requestDto = new ModUpdateCheckRequestDto(list, new ModUpdateCheckRequestDto.SemanticVersion(gamePackageInfo.versionName));
            OkGo.<List<ModUpdateCheckResponseDto>>post(Constants.UPDATE_CHECK_SERVICE_URL)
                    .upJson(JsonUtil.toJson(requestDto))
                    .execute(new JsonCallback<List<ModUpdateCheckResponseDto>>(new TypeReference<List<ModUpdateCheckResponseDto>>() {
                    }) {
                        @Override
                        public void onError(Response<List<ModUpdateCheckResponseDto>> response) {
                            super.onError(response);
                            checkUpdating.set(false);
                        }

                        @Override
                        public void onSuccess(Response<List<ModUpdateCheckResponseDto>> response) {
                            checkUpdating.set(false);
                            List<ModUpdateCheckResponseDto> checkResponseDtos = response.body();
                            if (checkResponseDtos != null) {
                                List<ModUpdateCheckResponseDto> list = checkResponseDtos.stream().filter(dto -> dto.getSuggestedUpdate() != null).collect(Collectors.toList());
                                callback.accept(list);
                            }
                        }
                    });
        } catch (Exception e) {
            checkUpdating.set(false);
            Crashes.trackError(e);
        }
    }

    private String checkModDependencyError(ModManifestEntry mod, ImmutableListMultimap<String, ModManifestEntry> installedModMap) {
        if (mod.getDependencies() != null) {
            List<ModManifestEntry> unsatisfiedDependencies = mod.getDependencies().stream()
                    .filter(dependency -> isDependencyIsExist(dependency, installedModMap))
                    .collect(Collectors.toList());
            if (unsatisfiedDependencies.size() > 0) {
                return root.getContext().getString(R.string.error_depends_on_mod, mod.getUniqueID(), Joiner.on(",").join(Lists.transform(unsatisfiedDependencies, ModManifestEntry::getUniqueID)));
            }
        }
        return null;
    }

    private boolean isDependencyIsExist(ModManifestEntry dependency, ImmutableListMultimap<String, ModManifestEntry> installedModMap) {
        if (dependency.getIsRequired() != null && !dependency.getIsRequired()) {
            return false;
        }
        ImmutableList<ModManifestEntry> entries = installedModMap.get(dependency.getUniqueID());
        if (entries.size() == 0) {
            for (String key : installedModMap.keySet()) {
                if (StringUtils.equalsIgnoreCase(key, dependency.getUniqueID())) {
                    dependency.setUniqueID(key);
                    entries = installedModMap.get(dependency.getUniqueID());
                    break;
                }
            }
        }
        if (entries.size() != 1) {
            return true;
        }
        String version = entries.get(0).getVersion();
        if (StringUtils.isBlank(version)) {
            return true;
        }
        if (StringUtils.isBlank(dependency.getMinimumVersion())) {
            return false;
        }
        if (VersionUtil.compareVersion(version, dependency.getMinimumVersion()) < 0) {
            return true;
        }
        return false;
    }

    private String checkContentPackDependencyError(ModManifestEntry mod, ImmutableListMultimap<String, ModManifestEntry> installedModMap) {
        ModManifestEntry dependency = mod.getContentPackFor();
        if (dependency != null) {
            if (dependency.getIsRequired() != null && !dependency.getIsRequired()) {
                return null;
            }
            ImmutableList<ModManifestEntry> entries = installedModMap.get(dependency.getUniqueID());
            if (entries.size() == 0) {
                for (String key : installedModMap.keySet()) {
                    if (StringUtils.equalsIgnoreCase(key, dependency.getUniqueID())) {
                        dependency.setUniqueID(key);
                        entries = installedModMap.get(dependency.getUniqueID());
                        break;
                    }
                }
            }
            if (entries.size() != 1) {
                return root.getContext().getString(R.string.error_depends_on_mod, mod.getUniqueID(), dependency.getUniqueID());
            }
            String version = entries.get(0).getVersion();
            if (!StringUtils.isBlank(version)) {
                if (StringUtils.isBlank(dependency.getMinimumVersion())) {
                    return null;
                }
                if (VersionUtil.compareVersion(version, dependency.getMinimumVersion()) < 0) {
                    return root.getContext().getString(R.string.error_depends_on_mod_version, mod.getUniqueID(), dependency.getUniqueID(), dependency.getMinimumVersion());
                }
            }
            return null;
        }
        return null;
    }
}

/*******************************************************************************
 * Copyright (C) 2014 xperia64 <xperiancedapps@gmail.com>
 * 
 * Copyright (C) 1999-2008 Masanao Izumo <iz@onicos.co.jp>
 *     
 * Copyright (C) 1995 Tuukka Toivonen <tt@cgs.fi>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;
import com.xperia64.timidityae.util.FileComparator;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.CommandStrings;
import com.xperia64.timidityae.util.SettingsStorage;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FileBrowserFragment extends ListFragment {
	public String currPath;
	List<String> path;
	List<String> fname;
	boolean gotDir = false;
	ActionFileBackListener mCallback;
	public boolean localfinished = false;

	public interface ActionFileBackListener {
		public void needFileBackCallback(boolean yes);
	}

	public static FileBrowserFragment create(String fold) {
		Bundle args = new Bundle();
		args.putString(CommandStrings.currFoldKey, fold);
		FileBrowserFragment fragment = new FileBrowserFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null)
			currPath = getArguments().getString(CommandStrings.currFoldKey);
		if (currPath == null)
			currPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		else if (!new File(currPath).exists())
			currPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.list, container, false);
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (!gotDir) {
			gotDir = true;
			getDir(currPath);
		}
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		try {
			mCallback = (ActionFileBackListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ActionFileBackListener");
		}
		if (Globals.shouldRestore) {
			Intent new_intent = new Intent();
			new_intent.setAction(CommandStrings.msrv_rec);
			new_intent.putExtra(CommandStrings.msrv_cmd, CommandStrings.msrv_cmd_get_fold);
			getActivity().sendBroadcast(new_intent);
		}
	}

	public void getDir(String dirPath) {
		currPath = dirPath;
		fname = new ArrayList<String>();
		path = new ArrayList<String>();
		if (currPath != null) {
			File f = new File(currPath);
			if (f.exists()) {
				File[] files = f.listFiles();
				if (files != null && files.length > 0) {

					Arrays.sort(files, new FileComparator());

					// System.out.println(currPath);
					if (!currPath.matches(Globals.repeatedSeparatorString)) {
						fname.add(Globals.parentString);
						// Thank you Marshmallow.
						// Disallowing access to /storage/emulated has now
						// prevent billions of hacking attempts daily.
						if (new File(f.getParent()).canRead()) {
							path.add(f.getParent() + File.separator);
						} else {
							path.add(File.separator);
						}
						mCallback.needFileBackCallback(true);
					} else {
						mCallback.needFileBackCallback(false);
					}
					for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if ((!file.getName().startsWith(".") && !SettingsStorage.showHiddenFiles) || SettingsStorage.showHiddenFiles) {
							if (file.isFile()) {
								int dotPosition = file.getName().lastIndexOf(".");
								String extension = "";
								if (dotPosition != -1) {
									extension = (file.getName().substring(dotPosition)).toLowerCase(Locale.US);
									if (extension != null) {

										if ((SettingsStorage.showVideos ? Globals.musicVideoFiles : Globals.musicFiles).contains("*" + extension + "*")) {

											path.add(file.getAbsolutePath());
											fname.add(file.getName());
										}
									} else if (file.getName().endsWith(File.separator)) {
										path.add(file.getAbsolutePath() + File.separator);
										fname.add(file.getName() + File.separator);
									}
								}
							} else {
								path.add(file.getAbsolutePath() + File.separator);
								fname.add(file.getName() + File.separator);
							}
						}
					}
				} else {
					if (!currPath.matches(Globals.repeatedSeparatorString)) {
						fname.add(Globals.parentString);
						path.add(f.getParent() + File.separator);

					}
				}
				ArrayAdapter<String> fileList = new ArrayAdapter<String>(getActivity(), R.layout.row, fname);
				getListView().setFastScrollEnabled(true);
				getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> l, View v, final int position, long id) {
						if (Globals.isMidi(path.get(position))) {
							((TimidityActivity) getActivity()).dynExport(path.get(position), false);
							return true;
						}
						return false;
					}

				});
				setListAdapter(fileList);
			}
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		File file = new File(path.get(position));
		if (file.isDirectory()) {
			if (file.canRead()) {
				getDir(path.get(position));
			} else {
				AlertDialog.Builder unreadableDialog = new AlertDialog.Builder(getActivity());
				unreadableDialog = unreadableDialog.setIcon(R.drawable.ic_launcher);
				unreadableDialog = unreadableDialog.setTitle(String.format("[%1$s] %2$s", file.getName(), getActivity().getResources().getString(R.string.fb_cread)));
				unreadableDialog = unreadableDialog.setPositiveButton(getActivity().getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				unreadableDialog.show();
			}
		} else {
			if (file.canRead()) {
				ArrayList<String> files = new ArrayList<String>();
				int firstFile = -1;
				for (int i = 0; i < path.size(); i++) {
					if (!path.get(i).endsWith(File.separator)) {
						files.add(path.get(i));
						if (firstFile == -1) {
							firstFile = i;
						}
					}
				}
				((TimidityActivity) getActivity()).selectedSong(files, position - firstFile, true, false, false);
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CommandStrings.currFoldKey, currPath);
	}
}

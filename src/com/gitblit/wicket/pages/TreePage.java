/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.wicket.pages;

import java.util.List;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.gitblit.DownloadZipServlet;
import com.gitblit.GitBlit;
import com.gitblit.Keys;
import com.gitblit.models.PathModel;
import com.gitblit.utils.ByteFormat;
import com.gitblit.utils.JGitUtils;
import com.gitblit.wicket.WicketUtils;
import com.gitblit.wicket.panels.CommitHeaderPanel;
import com.gitblit.wicket.panels.LinkPanel;
import com.gitblit.wicket.panels.PathBreadcrumbsPanel;

public class TreePage extends RepositoryPage {

	public TreePage(PageParameters params) {
		super(params);

		final String path = WicketUtils.getPath(params);

		Repository r = getRepository();
		RevCommit commit = getCommit();
		List<PathModel> paths = JGitUtils.getFilesInPath(r, path, commit);

		// tree page links
		add(new BookmarkablePageLink<Void>("historyLink", HistoryPage.class,
				WicketUtils.newPathParameter(repositoryName, objectId, path)));
		add(new BookmarkablePageLink<Void>("headLink", TreePage.class,
				WicketUtils.newPathParameter(repositoryName, Constants.HEAD, path)));
		add(new ExternalLink("zipLink", DownloadZipServlet.asLink(getRequest()
				.getRelativePathPrefixToContextRoot(), repositoryName, objectId, path))
				.setVisible(GitBlit.getBoolean(Keys.web.allowZipDownloads, true)));

		add(new CommitHeaderPanel("commitHeader", repositoryName, commit));

		// breadcrumbs
		add(new PathBreadcrumbsPanel("breadcrumbs", repositoryName, path, objectId));
		if (path != null && path.trim().length() > 0) {
			// add .. parent path entry
			String parentPath = null;
			if (path.lastIndexOf('/') > -1) {
				parentPath = path.substring(0, path.lastIndexOf('/'));
			}
			PathModel model = new PathModel("..", parentPath, 0, FileMode.TREE.getBits(), objectId);
			model.isParentPath = true;
			paths.add(0, model);
		}

		final ByteFormat byteFormat = new ByteFormat();

		final String baseUrl = WicketUtils.getGitblitURL(getRequest());

		// changed paths list
		ListDataProvider<PathModel> pathsDp = new ListDataProvider<PathModel>(paths);
		DataView<PathModel> pathsView = new DataView<PathModel>("changedPath", pathsDp) {
			private static final long serialVersionUID = 1L;
			int counter;

			public void populateItem(final Item<PathModel> item) {
				PathModel entry = item.getModelObject();
				item.add(new Label("pathPermissions", JGitUtils.getPermissionsFromMode(entry.mode)));
				if (entry.isParentPath) {
					// parent .. path
					item.add(WicketUtils.newBlankImage("pathIcon"));
					item.add(new Label("pathSize", ""));
					item.add(new LinkPanel("pathName", null, entry.name, TreePage.class,
							WicketUtils
									.newPathParameter(repositoryName, entry.commitId, entry.path)));
					item.add(new Label("pathLinks", ""));
				} else {
					if (entry.isTree()) {
						// folder/tree link
						item.add(WicketUtils.newImage("pathIcon", "folder_16x16.png"));
						item.add(new Label("pathSize", ""));
						item.add(new LinkPanel("pathName", "list", entry.name, TreePage.class,
								WicketUtils.newPathParameter(repositoryName, entry.commitId,
										entry.path)));

						// links
						Fragment links = new Fragment("pathLinks", "treeLinks", this);
						links.add(new BookmarkablePageLink<Void>("tree", TreePage.class,
								WicketUtils.newPathParameter(repositoryName, entry.commitId,
										entry.path)));
						links.add(new BookmarkablePageLink<Void>("history", HistoryPage.class,
								WicketUtils.newPathParameter(repositoryName, entry.commitId,
										entry.path)));
						links.add(new ExternalLink("zip", DownloadZipServlet.asLink(baseUrl,
								repositoryName, objectId, entry.path)).setVisible(GitBlit
								.getBoolean(Keys.web.allowZipDownloads, true)));
						item.add(links);
					} else {
						// blob link
						item.add(WicketUtils.getFileImage("pathIcon", entry.name));
						item.add(new Label("pathSize", byteFormat.format(entry.size)));
						item.add(new LinkPanel("pathName", "list", entry.name, BlobPage.class,
								WicketUtils.newPathParameter(repositoryName, entry.commitId,
										entry.path)));

						// links
						Fragment links = new Fragment("pathLinks", "blobLinks", this);
						links.add(new BookmarkablePageLink<Void>("view", BlobPage.class,
								WicketUtils.newPathParameter(repositoryName, entry.commitId,
										entry.path)));
						links.add(new BookmarkablePageLink<Void>("raw", RawPage.class, WicketUtils
								.newPathParameter(repositoryName, entry.commitId, entry.path)));
						links.add(new BookmarkablePageLink<Void>("blame", BlamePage.class,
								WicketUtils.newPathParameter(repositoryName, entry.commitId,
										entry.path)));
						links.add(new BookmarkablePageLink<Void>("history", HistoryPage.class,
								WicketUtils.newPathParameter(repositoryName, entry.commitId,
										entry.path)));
						item.add(links);
					}
				}
				WicketUtils.setAlternatingBackground(item, counter);
				counter++;
			}
		};
		add(pathsView);
	}

	@Override
	protected String getPageName() {
		return getString("gb.tree");
	}
}

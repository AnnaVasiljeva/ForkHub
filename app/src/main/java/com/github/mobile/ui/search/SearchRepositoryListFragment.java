/*
 * Copyright 2013 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mobile.ui.search;

import static android.app.SearchManager.QUERY;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.github.mobile.R;
import com.github.mobile.api.model.Repository;
import com.github.mobile.api.service.SearchService;
import com.github.mobile.core.ResourcePager;
import com.github.mobile.core.repo.RefreshRepositoryTask;
import com.github.mobile.ui.PagedItemFragment;
import com.github.mobile.ui.repo.RepositoryViewActivity;
import com.google.inject.Inject;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.service.RepositoryService;

/**
 * Fragment to display a list of {@link Repository} instances
 */
public class SearchRepositoryListFragment extends PagedItemFragment<Repository> {
    private static final int RESULTS_PER_PAGE = 30;

    @Inject
    private RepositoryService service;

    @Inject
    private SearchService searchService;

    private String query;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(R.string.no_repositories);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        query = getStringExtra(QUERY);
    }

    @Override
    public void refresh() {
        query = getStringExtra(QUERY);

        super.refresh();
    }

    @Override
    public void refreshWithProgress() {
        query = getStringExtra(QUERY);

        super.refreshWithProgress();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Repository result = (Repository) l.getItemAtPosition(position);
        final SearchRepository searchRepository = new SearchRepository(result.owner.login, result.name);
        new RefreshRepositoryTask(getActivity(), searchRepository) {

            @Override
            public void execute() {
                showIndeterminate(MessageFormat.format(
                        getString(R.string.opening_repository),
                        searchRepository.generateId()));

                super.execute();
            }

            @Override
            protected void onSuccess(org.eclipse.egit.github.core.Repository repository) throws Exception {
                super.onSuccess(repository);

                startActivity(RepositoryViewActivity.createIntent(repository));
            }
        }.execute();
    }

    @Override
    protected ResourcePager<Repository> createPager() {
        return new ResourcePager<Repository>() {

            @Override
            protected Object getId(Repository resource) {
                return resource.id;
            }

            @Override
            public Iterator<Collection<Repository>> createIterator(int page, int size) {
                return new com.github.mobile.api.PageIterator<Repository>(page, RESULTS_PER_PAGE) {

                    @Override
                    protected Collection<Repository> getPage(int page) {
                        if (openRepositoryMatch(query)) {
                            return Collections.emptyList();
                        }

                        try {
                            return searchService.searchRepositories(query, page, size).execute().body().items;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return Collections.emptyList();
                    }
                };
            }
        };
    }

    /**
     * Check if the search query is an exact repository name/owner match and
     * open the repository activity and finish the current activity when it is
     *
     * @param query
     * @return true if query opened as repository, false otherwise
     */
    private boolean openRepositoryMatch(final String query) {
        if (TextUtils.isEmpty(query))
            return false;

        RepositoryId repoId = RepositoryId.createFromId(query.trim());
        if (repoId == null)
            return false;

        org.eclipse.egit.github.core.Repository repo;
        try {
            repo = service.getRepository(repoId);
        } catch (IOException e) {
            return false;
        }

        startActivity(RepositoryViewActivity.createIntent(repo));
        final Activity activity = getActivity();
        if (activity != null)
            activity.finish();
        return true;
    }

    @Override
    protected int getLoadingMessage() {
        return R.string.loading_repositories;
    }

    @Override
    protected int getErrorMessage(Exception exception) {
        return R.string.error_repos_load;
    }

    @Override
    protected SingleTypeAdapter<Repository> createAdapter(
            List<Repository> items) {
        return new SearchRepositoryListAdapter(getActivity()
                .getLayoutInflater(), items.toArray(new Repository[items
                .size()]));
    }
}

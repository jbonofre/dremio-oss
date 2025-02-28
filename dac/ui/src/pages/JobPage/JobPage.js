/*
 * Copyright (C) 2017-2019 Dremio Corporation
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
import { PureComponent } from "react";
import { connect } from "react-redux";
import Immutable from "immutable";
import PropTypes from "prop-types";
import DocumentTitle from "react-document-title";
import { injectIntl } from "react-intl";
import { flexElementAuto } from "@app/uiTheme/less/layout.less";
import { getClusterInfo } from "@app/utils/infoUtils";
import { getSupport } from "@app/utils/supportUtils";

import {
  updateQueryState,
  filterJobsData,
  loadItemsForFilter,
  loadNextJobs,
  setClusterType,
} from "actions/jobs/jobs";

import { getJobs, getDataWithItemsForFilters } from "selectors/jobs";
import { getViewState } from "selectors/resources";
import { SonarSideNav } from "@app/exports/components/SideNav/SonarSideNav";

import { parseQueryState } from "utils/jobsQueryState";
import jobsUtils from "utils/jobsUtils";

import RunningJobsHeader from "./components/RunningJobsHeader";
import JobsContent from "./components/JobsContent";

import "./JobPage.less";

const VIEW_ID = "JOB_PAGE_VIEW_ID";

@injectIntl
export class JobPage extends PureComponent {
  static propTypes = {
    location: PropTypes.object.isRequired,
    jobId: PropTypes.string,
    jobs: PropTypes.instanceOf(Immutable.List).isRequired,
    queryState: PropTypes.instanceOf(Immutable.Map).isRequired,
    next: PropTypes.string,
    viewState: PropTypes.instanceOf(Immutable.Map),
    isNextJobsInProgress: PropTypes.bool,
    dataWithItemsForFilters: PropTypes.object,
    clusterType: PropTypes.string,
    admin: PropTypes.bool,

    //actions
    updateQueryState: PropTypes.func.isRequired,
    filterJobsData: PropTypes.func.isRequired,
    loadItemsForFilter: PropTypes.func,
    loadNextJobs: PropTypes.func,
    style: PropTypes.object,
    intl: PropTypes.object.isRequired,
    setClusterType: PropTypes.func,
  };

  componentDidMount() {
    this.receiveProps(this.props);
    if (this.props.clusterType === "NA") {
      this.handleCluster();
    }
  }

  componentWillReceiveProps(nextProps) {
    this.receiveProps(nextProps, this.props);
  }

  receiveProps(nextProps, prevProps = {}) {
    const { queryState } = nextProps;
    if (!Object.keys(nextProps.location.query).length) {
      // first load, or re-clicking "Jobs" in the header
      nextProps.updateQueryState(
        queryState.setIn(["filters", "qt"], ["UI", "EXTERNAL"])
      );
    } else if (!nextProps.queryState.equals(prevProps.queryState)) {
      nextProps.filterJobsData(nextProps.queryState, VIEW_ID);
    }
  }

  handleCluster = async () => {
    const clusterInfo = await getClusterInfo();
    const supportInfo =
      getSupport(this.props.admin) !== undefined
        ? getSupport(this.props.admin)
        : false;
    const data = {
      clusterType: clusterInfo.clusterType,
      isSupport: supportInfo,
    };
    clusterInfo.clusterType !== undefined
      ? this.props.setClusterType(data)
      : this.props.setClusterType("NP");
  };

  render() {
    const { jobId, jobs, queryState, viewState, style, location, intl } =
      this.props;
    const runningJobsCount = jobsUtils.getNumberOfRunningJobs(jobs);

    return (
      <div style={style}>
        <DocumentTitle title={intl.formatMessage({ id: "Job.Jobs" })} />
        <div className={"jobsPageBody"}>
          <SonarSideNav />
          <div className={"jobPageContentDiv"}>
            <RunningJobsHeader jobCount={runningJobsCount} />
            <JobsContent
              className={flexElementAuto} // Page object adds flex in style
              loadNextJobs={this.props.loadNextJobs}
              // todo: update to react-router v3 so don't have to deep pass `location` anymore
              location={location}
              jobId={jobId}
              jobs={jobs}
              queryState={queryState}
              next={this.props.next}
              isNextJobsInProgress={this.props.isNextJobsInProgress}
              viewState={viewState}
              onUpdateQueryState={this.props.updateQueryState}
              loadItemsForFilter={this.props.loadItemsForFilter}
              dataWithItemsForFilters={this.props.dataWithItemsForFilters}
            />
          </div>
        </div>
      </div>
    );
  }
}

function mapStateToProps(state, ownProps) {
  const { location } = ownProps;
  const jobId = location.hash && location.hash.slice(1);
  return {
    jobId,
    jobs: getJobs(state, ownProps),
    queryState: parseQueryState(location.query),
    next: state.jobs.jobs.get("next"),
    isNextJobsInProgress: state.jobs.jobs.get("isNextJobsInProgress"),
    dataWithItemsForFilters: getDataWithItemsForFilters(state),
    viewState: getViewState(state, VIEW_ID),
    clusterType: state.jobs.jobs.get("clusterType"),
    admin: state.account.get("user").get("admin"),
  };
}

export default connect(mapStateToProps, {
  updateQueryState,
  filterJobsData,
  loadItemsForFilter,
  loadNextJobs,
  setClusterType,
})(JobPage);

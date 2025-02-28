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

import PropTypes from "prop-types";
import Immutable from "immutable";
import { injectIntl } from "react-intl";
import { connect } from "react-redux";

import config from "dyn-load/utils/config";
import { getAnalyzeToolsConfig } from "@app/utils/config";
import { HANDLE_THROUGH_API } from "@inject/pages/HomePage/components/HeaderButtonConstants";
import MenuItem from "components/Menus/MenuItem";
import SubMenu from "components/Menus/SubMenu";
import AnalyzeMenuItems from "components/Menus/AnalyzeMenuItems";

import {
  openTableau,
  openQlikSense,
  openPowerBI,
} from "actions/explore/download";

@injectIntl
export class AnalyzeMenuItem extends PureComponent {
  static propTypes = {
    entity: PropTypes.instanceOf(Immutable.Map),
    openTableau: PropTypes.func,
    openQlikSense: PropTypes.func,
    openPowerBI: PropTypes.func,
    closeMenu: PropTypes.func,
    intl: PropTypes.object.isRequired,
  };

  handleTableauClick = () => {
    this.props.openTableau(this.props.entity);
    this.props.closeMenu();
  };

  handleQlikClick = () => {
    this.props.openQlikSense(this.props.entity);
    this.props.closeMenu();
  };

  handlePowerBIClick = () => {
    this.props.openPowerBI(this.props.entity);
    this.props.closeMenu();
  };

  haveEnabledTools = (analyzeToolsConfig) => {
    if (HANDLE_THROUGH_API) {
      const supportFlags = localStorage.getItem("supportFlags")
        ? JSON.parse(localStorage.getItem("supportFlags"))
        : null;

      return (
        supportFlags &&
        (supportFlags["client.tools.tableau"] ||
          supportFlags["client.tools.powerbi"] ||
          analyzeToolsConfig.qlik.enabled)
      );
    } else {
      return (
        analyzeToolsConfig.tableau.enabled ||
        analyzeToolsConfig.powerbi.enabled ||
        analyzeToolsConfig.qlik.enabled
      );
    }
  };

  render() {
    const analyzeToolsConfig = getAnalyzeToolsConfig(config);
    if (!this.haveEnabledTools(analyzeToolsConfig)) return null;

    return (
      <MenuItem
        rightIcon={
          <dremio-icon
            name="interface/triangle-right"
            alt={""}
            class={styles.rightIcon}
          />
        }
        menuItems={[
          <SubMenu key="analyze-with">
            <AnalyzeMenuItems
              openTableau={this.handleTableauClick}
              openPowerBI={this.handlePowerBIClick}
              openQlikSense={this.handleQlikClick}
              analyzeToolsConfig={analyzeToolsConfig}
            />
          </SubMenu>,
        ]}
      >
        {la("Analyze With")}
      </MenuItem>
    );
  }
}

export default connect(null, {
  openTableau,
  openQlikSense,
  openPowerBI,
})(AnalyzeMenuItem);

const styles = {
  rightIcon: {
    width: 25,
    height: 25,
    marginRight: -10,
  },
};

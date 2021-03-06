(ns status-im.ui.screens.pairing.views
  (:require-macros [status-im.utils.views :as views])
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.utils.config :as config]
            [status-im.ui.screens.main-tabs.styles :as main-tabs.styles]
            [status-im.ui.components.icons.vector-icons :as icons]
            [status-im.ui.screens.home.styles :as home.styles]
            [status-im.utils.platform :as utils.platform]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.ui.components.button.view :as buttons]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.status-bar.view :as status-bar]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.ui.components.toolbar.actions :as toolbar.actions]
            [status-im.ui.screens.profile.components.views :as profile.components]
            [status-im.ui.screens.pairing.styles :as styles]))

(defn icon-style [{:keys [width height] :as style}]
  (if utils.platform/desktop?
    {:container-style {:width width

                       :height height}
     :style  style}
    style))

(defn synchronize-installations! []
  (re-frame/dispatch [:pairing.ui/synchronize-installation-pressed]))

(defn pair! []
  (re-frame/dispatch [:pairing.ui/pair-devices-pressed]))

(defn enable-installation! [installation-id _]
  (re-frame/dispatch [:pairing.ui/enable-installation-pressed installation-id]))

(defn disable-installation! [installation-id _]
  (re-frame/dispatch [:pairing.ui/disable-installation-pressed installation-id]))

(defn footer []
  [react/touchable-highlight {:on-press synchronize-installations!
                              :style main-tabs.styles/tabs-container}
   [react/view
    {:style styles/footer-content}
    [react/text
     {:style styles/footer-text}
     (i18n/label :t/sync-all-devices)]]])

(defn pair-this-device []
  [react/touchable-highlight {:on-press pair!
                              :style styles/pair-this-device}
   [react/view {:style styles/pair-this-device-actions}
    [react/view
     [react/view (styles/pairing-button true)
      [icons/icon :icons/add (icon-style (styles/pairing-button-icon true))]]]
    [react/view {:style styles/pairing-actions-text}
     [react/view
      [react/text {:style styles/pair-this-device-title} (i18n/label :t/pair-this-device)]]
     [react/view
      [react/text (i18n/label :t/pair-this-device-description)]]]]])

(defn sync-devices []
  [react/touchable-highlight {:on-press synchronize-installations!
                              :style styles/pair-this-device}
   [react/view {:style styles/pair-this-device-actions}
    [react/view
     [react/view (styles/pairing-button true)
      [icons/icon :icons/wnode (icon-style (styles/pairing-button-icon true))]]]
    [react/view {:style styles/pairing-actions-text}
     [react/view
      [react/text {:style styles/pair-this-device-title} (i18n/label :t/sync-all-devices)]]]]])

(defn render-row [{:keys [device-type enabled? installation-id]}]
  [react/touchable-highlight
   {:on-press (if enabled?
                (partial disable-installation! installation-id)
                (partial enable-installation! installation-id))
    :accessibility-label :installation-item}
   [react/view {:style styles/installation-item}
    [react/view {:style (styles/pairing-button enabled?)}
     [icons/icon (if (= "desktop"
                        device-type)
                   :icons/desktop
                   :icons/mobile)
      (icon-style (styles/pairing-button-icon enabled?))]]
    [react/view {:style styles/pairing-actions-text}
     [react/view
      [react/text {:style styles/installation-item-name-text}
       (gfycat/generate-gfy installation-id)]]
     [react/view
      [react/text {:style styles/installation-status}
       (if enabled?
         (i18n/label :t/syncing-enabled)
         (i18n/label :t/syncing-disabled))]]]]])

(defn render-rows [installations]
  [react/view {:style styles/wrapper}
   [list/flat-list {:data               installations
                    :default-separator? false
                    :key-fn             :installation-id
                    :render-fn          render-row}]])

(defn installations-list [installations]
  [react/view {:style styles/installation-list}
   [react/view {:style styles/paired-devices-title}
    [react/text (i18n/label :t/paired-devices)]]
   (render-rows installations)])

(views/defview installations []
  (views/letsubs [installations [:pairing/installations]]
    [react/view {:flex 1}
     [status-bar/status-bar]
     [toolbar/toolbar {}
      toolbar/default-nav-back
      [toolbar/content-title (i18n/label :t/devices)]]
     [react/scroll-view {:style {:background-color :white}}
      [pair-this-device]
      (when (seq installations)
        [installations-list installations])]
     (when (seq installations) [footer])]))

jQuery(document).ready(function() {
  registerJsTab();
});

function registerJsTab() {
  jQuery('.js-tab').each(function() {
    jQuery(this).click(function() {
      onTabClick(this);
    });
  });
}

function onTabClick(tab) {
  jQuery(tab).parent().siblings("li").children("a").removeClass('is-active');
  jQuery(tab).addClass("is-active");
  jQuery(tab).parents('.tabs--lined').children('.tabs__content')
    .children('div').addClass('is-hidden');
  jQuery(jQuery(tab).attr('href') + '_content').removeClass('is-hidden');
}

function checkHashUrl(defaultTabId, defaultSettingsTabId) {
  var originalHashUrl = window.location.hash;

  if (window.location.hash) {
    if (window.location.hash.substring(0, 9) == '#settings') {
      window.location.hash = "#settings";
    }

    if (elementExist(window.location.hash + "_tab")) {
      defaultTabId = window.location.hash + "_tab";
    }
  }
  onTabClick(defaultTabId);
  window.location.hash = defaultTabId.replace("_tab", "");

  if (window.location.hash.substring(0, 9) == "#settings") {
    handleSettingsTab(defaultSettingsTabId, originalHashUrl);
  }
}

function handleSettingsTab(defaultSettingsTabId, hashUrl) {
  var selectedSettingsTabId = defaultSettingsTabId;
  if (elementExist(hashUrl)) {
    selectedSettingsTabId = hashUrl + "_tab";
  }
  jQuery(selectedSettingsTabId)[0].click();
}

function elementExist(hashId) {
  return jQuery(hashId).length != 0;
}
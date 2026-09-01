$(function() {
	"use strict";

	function initPerfectScrollbar(selector) {
		var el = document.querySelector(selector);
		if (!el || typeof PerfectScrollbar !== "function") {
			return;
		}
		try {
			new PerfectScrollbar(el);
		} catch (e) {}
	}
	initPerfectScrollbar(".header-message-list");
	initPerfectScrollbar(".header-notifications-list");

	    $(".mobile-search-icon").on("click", function() {
			$(".search-bar").addClass("full-search-bar")
		}),

		$(".search-close").on("click", function() {
			$(".search-bar").removeClass("full-search-bar")
		}),

        $(".mobile-toggle-menu").on("click", function() {
			$(".wrapper").toggleClass("toggled");
			$("body").toggleClass("pb-sidebar-open", $(".wrapper").hasClass("toggled"));
		}),

		$(".overlay").on("click", function() {
			if (window.innerWidth < 1025) {
				$(".wrapper").removeClass("toggled sidebar-hovered");
				$("body").removeClass("pb-sidebar-open");
				$(".sidebar-wrapper").unbind("hover");
			}
		}),
		
		
		$(".toggle-icon, .sidebar-collapse-btn").click(function() {
			if (window.innerWidth < 1025) {
				return;
			}
			$(".wrapper").hasClass("toggled") ? ($(".wrapper").removeClass("toggled"), $(".sidebar-wrapper").unbind("hover")) : ($(".wrapper").addClass("toggled"), $(".sidebar-wrapper").hover(function() {
				$(".wrapper").addClass("sidebar-hovered")
			}, function() {
				$(".wrapper").removeClass("sidebar-hovered")
			}))
		}),

		// Keep sidebar expanded on desktop by default
		$(window).on("load resize", function() {
			if (window.innerWidth >= 1025) {
				$(".wrapper").removeClass("toggled sidebar-hovered");
				$("body").removeClass("pb-sidebar-open");
				$(".sidebar-wrapper").unbind("hover");
			} else {
				$(".wrapper").removeClass("sidebar-hovered");
			}
		}),
		$(document).ready(function() {
			$(window).on("scroll", function() {
				$(this).scrollTop() > 300 ? $(".back-to-top").fadeIn() : $(".back-to-top").fadeOut()
			}), $(".back-to-top").on("click", function() {
				return $("html, body").animate({
					scrollTop: 0
				}, 600), !1
			})
		}),
		$(function() {
			var $menu = $("#menu");
			if (!$menu.length) {
				return;
			}

			$menu.find("> li > ul").addClass("mm-collapse");

			var path = window.location.pathname.replace(/\/+$/, "") || "/";
			var $current = $menu.find("li.pb-nav-current").first();

			if (!$current.length) {
				var $matches = $menu.find("li a[href]").filter(function() {
					var href = this.getAttribute("href");
					if (!href || href.indexOf("javascript:") === 0) {
						return false;
					}
					try {
						var linkPath = new URL(this.href, window.location.origin).pathname.replace(/\/+$/, "") || "/";
						return linkPath === path;
					} catch (e) {
						return false;
					}
				});

				var $link = $matches.filter(function() {
					return $(this).parent("li").parent("ul").is($menu);
				}).first();
				if (!$link.length) {
					$link = $matches.first();
				}
				if ($link.length) {
					$current = $link.closest("li").addClass("pb-nav-current");
				}
			}

			if ($current.length) {
				var $ul = $current.parent("ul");
				while ($ul.length && !$ul.is($menu)) {
					$ul.addClass("mm-show mm-collapse");
					$ul.parent("li").addClass("mm-active");
					$ul = $ul.parent("li").parent("ul");
				}
			}

			$menu.off("click.pbNav").on("click.pbNav", "> li > a.has-arrow", function (e) {
				e.preventDefault();
				e.stopImmediatePropagation();
				var $li = $(this).closest("li");
				var $submenu = $li.children("ul");
				if (!$submenu.length) {
					return;
				}
				var willOpen = !$submenu.hasClass("mm-show");
				$menu.children("li").not($li).removeClass("mm-active").children("ul").removeClass("mm-show");
				$li.toggleClass("mm-active", willOpen);
				$submenu.toggleClass("mm-show", willOpen);
			});

			$menu.off("click.pbNavLink").on("click.pbNavLink", "a[href]:not(.has-arrow)", function () {
				if (window.innerWidth < 1025) {
					$(".wrapper").removeClass("toggled sidebar-hovered");
					$("body").removeClass("pb-sidebar-open");
				}
			});
		}), $(".chat-toggle-btn").on("click", function() {
			$(".chat-wrapper").toggleClass("chat-toggled")
		}), $(".chat-toggle-btn-mobile").on("click", function() {
			$(".chat-wrapper").removeClass("chat-toggled")
		}), $(".email-toggle-btn").on("click", function() {
			$(".email-wrapper").toggleClass("email-toggled")
		}), $(".email-toggle-btn-mobile").on("click", function() {
			$(".email-wrapper").removeClass("email-toggled")
		}), $(".compose-mail-btn").on("click", function() {
			$(".compose-mail-popup").show()
		}), $(".compose-mail-close").on("click", function() {
			$(".compose-mail-popup").hide()
		}), $(".switcher-btn").on("click", function() {
			$(".switcher-wrapper").toggleClass("switcher-toggled")
		}), $(".close-switcher").on("click", function() {
			$(".switcher-wrapper").removeClass("switcher-toggled")
		}), $("#lightmode").on("click", function() {
			$("html").attr("class", "light-theme")
		}), $("#darkmode").on("click", function() {
			$("html").attr("class", "dark-theme")
		}), $("#semidark").on("click", function() {
			$("html").attr("class", "semi-dark")
		}), $("#minimaltheme").on("click", function() {
			$("html").attr("class", "minimal-theme")
		}), $("#headercolor1").on("click", function() {
			$("html").addClass("color-header headercolor1"), $("html").removeClass("headercolor2 headercolor3 headercolor4 headercolor5 headercolor6 headercolor7 headercolor8")
		}), $("#headercolor2").on("click", function() {
			$("html").addClass("color-header headercolor2"), $("html").removeClass("headercolor1 headercolor3 headercolor4 headercolor5 headercolor6 headercolor7 headercolor8")
		}), $("#headercolor3").on("click", function() {
			$("html").addClass("color-header headercolor3"), $("html").removeClass("headercolor1 headercolor2 headercolor4 headercolor5 headercolor6 headercolor7 headercolor8")
		}), $("#headercolor4").on("click", function() {
			$("html").addClass("color-header headercolor4"), $("html").removeClass("headercolor1 headercolor2 headercolor3 headercolor5 headercolor6 headercolor7 headercolor8")
		}), $("#headercolor5").on("click", function() {
			$("html").addClass("color-header headercolor5"), $("html").removeClass("headercolor1 headercolor2 headercolor4 headercolor3 headercolor6 headercolor7 headercolor8")
		}), $("#headercolor6").on("click", function() {
			$("html").addClass("color-header headercolor6"), $("html").removeClass("headercolor1 headercolor2 headercolor4 headercolor5 headercolor3 headercolor7 headercolor8")
		}), $("#headercolor7").on("click", function() {
			$("html").addClass("color-header headercolor7"), $("html").removeClass("headercolor1 headercolor2 headercolor4 headercolor5 headercolor6 headercolor3 headercolor8")
		}), $("#headercolor8").on("click", function() {
			$("html").addClass("color-header headercolor8"), $("html").removeClass("headercolor1 headercolor2 headercolor4 headercolor5 headercolor6 headercolor7 headercolor3")
		})
		
	// sidebar colors 
	$('#sidebarcolor1').click(theme1);
	$('#sidebarcolor2').click(theme2);
	$('#sidebarcolor3').click(theme3);
	$('#sidebarcolor4').click(theme4);
	$('#sidebarcolor5').click(theme5);
	$('#sidebarcolor6').click(theme6);
	$('#sidebarcolor7').click(theme7);
	$('#sidebarcolor8').click(theme8);

	function theme1() {
		$('html').attr('class', 'color-sidebar sidebarcolor1');
	}

	function theme2() {
		$('html').attr('class', 'color-sidebar sidebarcolor2');
	}

	function theme3() {
		$('html').attr('class', 'color-sidebar sidebarcolor3');
	}

	function theme4() {
		$('html').attr('class', 'color-sidebar sidebarcolor4');
	}

	function theme5() {
		$('html').attr('class', 'color-sidebar sidebarcolor5');
	}

	function theme6() {
		$('html').attr('class', 'color-sidebar sidebarcolor6');
	}

	function theme7() {
		$('html').attr('class', 'color-sidebar sidebarcolor7');
	}

	function theme8() {
		$('html').attr('class', 'color-sidebar sidebarcolor8');
	}
});
package com.liskovsoft.smartyoutubetv2.common.vot;

import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.track.MediaTrack;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VotAudioTrackHelperTest {

    private static class TestFormatItem implements FormatItem {
        private final int id;
        private final String lang;
        private final boolean selected;
        private final boolean isDef;

        TestFormatItem(int id, String lang, boolean selected, boolean isDef) {
            this.id = id;
            this.lang = lang;
            this.selected = selected;
            this.isDef = isDef;
        }

        @Override public int getId() { return id; }
        @Override public String getFormatId() { return String.valueOf(id); }
        @Override public CharSequence getTitle() { return lang; }
        @Override public boolean isDefault() { return isDef; }
        @Override public boolean isSelected() { return selected; }
        @Override public boolean isPreset() { return false; }
        @Override public float getFrameRate() { return 0; }
        @Override public String getLanguage() { return lang; }
        @Override public int getWidth() { return 0; }
        @Override public int getHeight() { return 0; }
        @Override public int getType() { return TYPE_AUDIO; }
        @Override public MediaTrack getTrack() { return null; }
    }

    @Test
    public void testNativeRussianVideoNotDubbed() {
        FormatItem ruOriginal = new TestFormatItem(1, "ru (original)", true, true);
        List<FormatItem> formats = Arrays.asList(ruOriginal);

        VotAudioTrackHelper.TrackInfo info = VotAudioTrackHelper.from(ruOriginal);
        assertTrue(VotAudioTrackHelper.isRussianLang(info.langCode));
        assertTrue(VotAudioTrackHelper.isRussianOriginal(info));
        assertFalse("Native Russian video must not be flagged as Russian dubbed track",
                VotAudioTrackHelper.isRussianDubbedTrack(info, formats));
    }

    @Test
    public void testNativeRussianWithoutExplicitTagNotDubbed() {
        FormatItem ruPlain = new TestFormatItem(1, "ru", true, true);
        List<FormatItem> formats = Arrays.asList(ruPlain);

        VotAudioTrackHelper.TrackInfo info = VotAudioTrackHelper.from(ruPlain);
        assertTrue(VotAudioTrackHelper.isRussianLang(info.langCode));
        assertFalse("Plain Russian single track must not be flagged as dubbed",
                VotAudioTrackHelper.isRussianDubbedTrack(info, formats));
    }

    @Test
    public void testNativeRussianWithEnglishDubNotDubbed() {
        FormatItem ruOriginal = new TestFormatItem(1, "ru (original)", true, true);
        FormatItem enDubbed = new TestFormatItem(2, "en (dubbed)", false, false);
        List<FormatItem> formats = Arrays.asList(ruOriginal, enDubbed);

        VotAudioTrackHelper.TrackInfo info = VotAudioTrackHelper.from(ruOriginal);
        assertFalse("Original Russian track on multi-audio must not be flagged as Russian dubbed track",
                VotAudioTrackHelper.isRussianDubbedTrack(info, formats));
    }

    @Test
    public void testEnglishVideoWithRussianDubbedIsDetected() {
        FormatItem enOriginal = new TestFormatItem(1, "en (original)", false, true);
        FormatItem ruDubbed = new TestFormatItem(2, "ru (dubbed)", true, false);
        List<FormatItem> formats = Arrays.asList(enOriginal, ruDubbed);

        VotAudioTrackHelper.TrackInfo current = VotAudioTrackHelper.from(ruDubbed);
        assertTrue("Russian dub track on English video must be detected",
                VotAudioTrackHelper.isRussianDubbedTrack(current, formats));

        FormatItem bestOriginal = VotAudioTrackHelper.findBestOriginalForYandex(formats);
        assertNotNull(bestOriginal);
        assertEquals(1, bestOriginal.getId());
        assertEquals("en (original)", bestOriginal.getLanguage());
    }

    @Test
    public void testEnglishVideoWithRussianAutoDubIsDetected() {
        FormatItem enOriginal = new TestFormatItem(1, "en (original)", false, true);
        FormatItem ruAutoDub = new TestFormatItem(2, "ru (dubbed-auto)", true, false);
        List<FormatItem> formats = Arrays.asList(enOriginal, ruAutoDub);

        VotAudioTrackHelper.TrackInfo current = VotAudioTrackHelper.from(ruAutoDub);
        assertTrue("Russian auto-dub track on English video must be detected",
                VotAudioTrackHelper.isRussianDubbedTrack(current, formats));

        FormatItem bestOriginal = VotAudioTrackHelper.findBestOriginalForYandex(formats);
        assertNotNull(bestOriginal);
        assertEquals("en (original)", bestOriginal.getLanguage());
    }

    @Test
    public void testSpanishVideoWithRussianDubIsDetected() {
        FormatItem esOriginal = new TestFormatItem(1, "es (original)", false, true);
        FormatItem ruDubbed = new TestFormatItem(2, "ru (dubbed)", true, false);
        List<FormatItem> formats = Arrays.asList(esOriginal, ruDubbed);

        VotAudioTrackHelper.TrackInfo current = VotAudioTrackHelper.from(ruDubbed);
        assertTrue(VotAudioTrackHelper.isRussianDubbedTrack(current, formats));

        FormatItem bestOriginal = VotAudioTrackHelper.findBestOriginalForYandex(formats);
        assertNotNull(bestOriginal);
        assertEquals("es (original)", bestOriginal.getLanguage());
    }

    @Test
    public void testEnglishTrackSelectedNotRussianDub() {
        FormatItem enOriginal = new TestFormatItem(1, "en (original)", true, true);
        List<FormatItem> formats = Arrays.asList(enOriginal);

        VotAudioTrackHelper.TrackInfo current = VotAudioTrackHelper.from(enOriginal);
        assertFalse("English track must not be flagged as Russian dubbed",
                VotAudioTrackHelper.isRussianDubbedTrack(current, formats));
    }

    @Test
    public void testAutoTranslateLeavesRussianDubUntouched() {
        FormatItem enOriginal = new TestFormatItem(1, "en (original)", false, true);
        FormatItem ruDubbed = new TestFormatItem(2, "ru (dubbed)", true, false);
        List<FormatItem> formats = Arrays.asList(enOriginal, ruDubbed);

        VotAudioTrackHelper.TrackInfo current = VotAudioTrackHelper.from(ruDubbed);
        // shouldAutoStartLikeManual should return false when Russian track is active
        assertFalse("Auto VOT must not auto-start when Russian dub is selected",
                VotAudioTrackHelper.shouldAutoStartLikeManual(current, formats));
    }
}

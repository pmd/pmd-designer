/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/*
 * Diff Match and Patch
 *
 * Copyright 2006 Google Inc.
 * http://code.google.com/p/google-diff-match-patch/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sourceforge.pmd.util.fxdesigner.util;


import java.util.LinkedList;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Patch;

/*
 * Functions for diff, match and patch.
 * Computes the difference between two texts to create a patch.
 * Applies the patch onto another text, allowing for errors.
 *
 * @author fraser@google.com (Neil Fraser)
 */


/**
 * Wrapper around a DiffMatchPatch which augments the patchApply function
 * with an external handler to apply a patch to the contents of a text area.
 *
 * <p>This is quite a hack, nearly 100% copy-paste. I hope there's no problem with license -- C. Fournier
 *
 */
public class DiffMatchPatchWithHooks {

    /**
     * The number of bits in an int.
     */
    private final short matchMaxBits = 32;
    private final DiffMatchPatch dmp = new DiffMatchPatch();


    /**
     * Merge a set of patches onto the text.  Return a patched text, as well
     * as an array of true/false values indicating which patches were applied.
     *
     * @param patches         Array of Patch objects
     * @param text            Old text.
     * @param externalHandler Replacement function
     *
     * @return Two element Object array, containing the new text and an array of
     * boolean values.
     */
    public Object[] patchApply(LinkedList<Patch> patches, String text,
                               TextReplacementFunction externalHandler) {
        if (patches.isEmpty()) {
            return new Object[]{text, new boolean[0]};
        }

        // Deep copy the patches so that no changes are made to originals.
        patches = dmp.patchDeepCopy(patches);

        String nullPadding = dmp.patchAddPadding(patches);
        text = nullPadding + text + nullPadding;
        dmp.patchSplitMax(patches);

        int x = 0;
        // delta keeps track of the offset between the expected and actual location
        // of the previous patch.  If there are patches expected at positions 10 and
        // 20, but the first patch was found at 12, delta is 2 and the second patch
        // has an effective expected position of 22.
        int delta = 0;
        boolean[] results = new boolean[patches.size()];
        for (DiffMatchPatch.Patch aPatch : patches) {
            int expectedLoc = aPatch.start2 + delta;
            String text1 = dmp.diffText1(aPatch.diffs);
            int startLoc;
            int endLoc = -1;
            if (text1.length() > this.matchMaxBits) {
                // patchSplitMax will only provide an oversized pattern in the case of
                // a monster delete.
                startLoc = dmp.matchMain(text,
                                         text1.substring(0, this.matchMaxBits), expectedLoc);
                if (startLoc != -1) {
                    endLoc = dmp.matchMain(text,
                                           text1.substring(text1.length() - this.matchMaxBits),
                                           expectedLoc + text1.length() - this.matchMaxBits);
                    if (endLoc == -1 || startLoc >= endLoc) {
                        // Can't find valid trailing context.  Drop this patch.
                        startLoc = -1;
                    }
                }
            } else {
                startLoc = dmp.matchMain(text, text1, expectedLoc);
            }
            if (startLoc == -1) {
                // No match found.  :(
                results[x] = false;
                // Subtract the delta for this failed patch from subsequent patches.
                delta -= aPatch.length2 - aPatch.length1;
            } else {
                // Found a match.  :)
                results[x] = true;
                delta = startLoc - expectedLoc;
                String text2;
                if (endLoc == -1) {
                    text2 = text.substring(startLoc,
                                           Math.min(startLoc + text1.length(), text.length()));
                } else {
                    text2 = text.substring(startLoc,
                                           Math.min(endLoc + this.matchMaxBits, text.length()));
                }
                if (text1.equals(text2)) {
                    // Perfect match, just shove the replacement text in.
                    String diffs = dmp.diffText2(aPatch.diffs);
                    externalHandler.replace(startLoc - nullPadding.length(), startLoc + text1.length() - nullPadding.length(), diffs);
                    text = text.substring(0, startLoc) + diffs
                            + text.substring(startLoc + text1.length());
                } else {
                    // Imperfect match.  Run a diff to get a framework of equivalent
                    // indices.
                    LinkedList<Diff> diffs = dmp.diffMain(text1, text2, false);
                    if (text1.length() > this.matchMaxBits
                            && dmp.diffLevenshtein(diffs) / (float) text1.length()
                            > dmp.patchDeleteThreshold) {
                        // The end points match, but the content is unacceptably bad.
                        results[x] = false;
                    } else {
                        dmp.diffCleanupSemanticLossless(diffs);
                        int index1 = 0;
                        for (DiffMatchPatch.Diff aDiff : aPatch.diffs) {
                            if (aDiff.operation != DiffMatchPatch.Operation.EQUAL) {
                                int index2 = dmp.diffXIndex(diffs, index1);
                                if (aDiff.operation == DiffMatchPatch.Operation.INSERT) {
                                    // Insertion
                                    int insertionPoint = startLoc + index2;
                                    externalHandler.replace(insertionPoint - nullPadding.length(), insertionPoint - nullPadding.length(), aDiff.text);
                                    text = text.substring(0, insertionPoint) + aDiff.text
                                            + text.substring(insertionPoint);
                                } else if (aDiff.operation == DiffMatchPatch.Operation.DELETE) {
                                    // Deletion
                                    int startDelete = startLoc + index2;
                                    int endDelete = startLoc + dmp.diffXIndex(diffs,
                                                                              index1 + aDiff.text.length());
                                    externalHandler.replace(startDelete - nullPadding.length(), endDelete - nullPadding.length(), "");
                                    text = text.substring(0, startDelete)
                                            + text.substring(endDelete);
                                }
                            }
                            if (aDiff.operation != DiffMatchPatch.Operation.DELETE) {
                                index1 += aDiff.text.length();
                            }
                        }
                    }
                }
            }
            x++;
        }
        // Strip the padding off.
        text = text.substring(nullPadding.length(), text.length()
                - nullPadding.length());
        return new Object[]{text, results};
    }


    public LinkedList<Patch> patchMake(String text1, String text2) {
        return dmp.patchMake(text1, text2);
    }


    @FunctionalInterface
    public interface TextReplacementFunction {
        void replace(int start, int end, String text);
    }


}

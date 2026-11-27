"""Species-candidate retrieval from UCI Leaf specimen descriptors, with open-set rejection.

Input is the original 16-column leaf.csv. Species and specimen numbers are never features.
Class IDs refer to the UCI codebook, never Plantupia identity records.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np


def evaluate(path, *, whiten=False):
    rows = np.loadtxt(path, delimiter=",")
    if rows.ndim != 2 or rows.shape[1] != 16 or not np.isfinite(rows).all():
        raise ValueError("Expected complete UCI Leaf descriptor rows.")
    labels = rows[:, 0].astype(int)
    rng = np.random.default_rng(20261126)
    classes = rng.permutation(np.unique(labels))
    unknown = set(classes[::5])
    train, calibrate, test = [], [], []
    for label in classes:
        indices = rng.permutation(np.flatnonzero(labels == label))
        if label in unknown:
            test.extend(indices)
        else:
            train.extend(indices[4:])
            calibrate.extend(indices[:2])
            test.extend(indices[2:4])
    descriptors = rows[:, 2:]
    scale = descriptors[train].std(axis=0)
    z = (descriptors-descriptors[train].mean(axis=0))/np.where(scale > 0, scale, 1)
    if whiten:
        # Fit only on reference specimens. Shrink correlations toward the
        # identity so small-sample texture covariance stays invertible.
        covariance = np.cov(z[train], rowvar=False)
        regularized = 0.5 * covariance + 0.5 * np.eye(covariance.shape[0])
        z = np.linalg.solve(np.linalg.cholesky(regularized), z.T).T

    def retrieve(i):
        distances = np.linalg.norm(z[train]-z[i], axis=1)
        candidates = sorted((float(distances[labels[train] == label].min()), int(label))
                            for label in classes if label not in unknown)
        return candidates[0][1], candidates[0][0], candidates[0][0] / max(candidates[1][0], 1e-10)
    # Learn the distance cutoff on known calibration specimens only. Ratio is fixed in advance.
    cutoff = float(np.quantile([retrieve(i)[1] for i in calibrate], .90))
    outcomes = []
    for i in test:
        candidate, distance, ratio = retrieve(i)
        outcomes.append(dict(species=int(labels[i]), specimen=int(rows[i,1]), candidate=candidate,
                             unknown=bool(labels[i] in unknown), distance=distance, ratio=ratio,
                             accepted=bool(distance <= cutoff and ratio <= .80)))
    known = [v for v in outcomes if not v["unknown"]]
    unknown_rows = [v for v in outcomes if v["unknown"]]
    accepted = [v for v in known if v["accepted"]]
    correct = lambda v: v["species"] == v["candidate"]
    return dict(source="UCI Leaf, Silva and Marçal (2013), DOI 10.24432/C53C78",
                metric="shrinkage_whitened" if whiten else "standardized_euclidean",
                sha256=hashlib.sha256(path.read_bytes()).hexdigest(), numpy=np.__version__,
                train_n=len(train), calibration_n=len(calibrate), held_out_species=sorted(map(int, unknown)),
                known_test_n=len(known), unknown_test_n=len(unknown_rows), distance_cutoff=cutoff,
                known_top1=sum(map(correct, known))/len(known), known_coverage=len(accepted)/len(known),
                accepted_known_accuracy=sum(map(correct, accepted))/len(accepted) if accepted else None,
                unknown_false_accepts=sum(v["accepted"] for v in unknown_rows), rows=outcomes)


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("leaf_csv", type=Path)
    p.add_argument("output", type=Path)
    p.add_argument("--whiten", action="store_true", help="fit a shrinkage-whitened distance on the reference split")
    args = p.parse_args()
    args.output.write_text(json.dumps(evaluate(args.leaf_csv, whiten=args.whiten), indent=2) + "\n", encoding="utf-8")
